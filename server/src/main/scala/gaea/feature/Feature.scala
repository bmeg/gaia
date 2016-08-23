package gaea.feature

import com.thinkaurelius.titan.core.TitanGraph
import gremlin.scala._
import gaea.collection.Collection._
import org.apache.tinkerpop.gremlin.process.traversal.P._

object Feature {
  val Name = Key[String]("name")
  val synonymPrefix = "feature:"

  val individualStep = StepLabel[Vertex]()
  val variantStep = StepLabel[Vertex]()
  val featureStep = StepLabel[Vertex]()

  def removePrefix(name: String): String = {
    name.split(":").drop(1).reduceLeft((t, s) => t + ":" + s)
  }

  def synonymQuery(graph: TitanGraph) (name: String): GremlinScala[Vertex, shapeless.HNil] = {
    // graph.V.hasLabel("featureSynonym").has(Name, name).out("synonymFor")
    graph.V.hasLabel("featureSynonym").has(Name, synonymPrefix + name).out("synonymFor")
  }

  def synonymsQuery(graph: TitanGraph) (names: Seq[String]): GremlinScala[Vertex, shapeless.HNil] = {
    graph.V.hasLabel("featureSynonym").has(Name, within(names.map(synonymPrefix + _):_*)).out("synonymFor")
  }

  def findSynonymVertex(graph: TitanGraph) (name: String): Option[Vertex] = {
    synonymQuery(graph) (name).headOption
  }

  def findSynonym(graph: TitanGraph) (name: String): Option[String] = {
    val values = findSynonymVertex(graph) (name).map(_.valueMap())
    values.map(vertex => removePrefix(vertex("name").asInstanceOf[String]))
  }

  def findFeature(graph: TitanGraph) (name: String): Vertex = {
    findSynonymVertex(graph) (name).getOrElse {
      val synonym = graph.V.hasLabel("featureSynonym").has(Name, name).headOption.getOrElse {
        graph + ("featureSynonym", Name -> name)
      }

      val feature = graph.V.hasLabel("feature").has(Name, name).headOption.getOrElse {
        graph + ("feature", Name -> name)
      }

      synonym --- ("synonymFor") --> feature
      feature
    }
  }

  def findIndividualsWithVariants(graph: TitanGraph) (feature: String): GremlinScala[Vertex, shapeless.HNil] = {
    graph.V
      .hasLabel("feature")
      .has(Name, feature)
      .in("inFeature")
      .out("effectOf")
      .out("tumorSample")
      .out("sampleOf")
  }

  def findTumors(graph: TitanGraph) (feature: String): List[String] = {
    findIndividualsWithVariants(graph) (feature)
      .value[String]("submittedTumorSite")
      .toList
  }

  def findTumorCounts(graph: TitanGraph) (feature: String): Map[String, Int] = {
    groupCount[String](findTumors(graph) (feature))
  }

  def findVariantsForIndividuals(graph: TitanGraph) (individuals: Seq[String]) (genes: Seq[String]): Seq[Tuple3[String, String, String]] = {
    val query = graph.V.hasLabel("individual")
      .has(Name, within(individuals:_*)).as(individualStep)
      .in("sampleOf")
      .in("tumorSample").as(variantStep)
      .in("effectOf")
      .out("inFeature")
      .has(Name, within(genes:_*)).as(featureStep)
      .select((individualStep, variantStep, featureStep))
      .toList

    query.map { q =>
      val (individual, variant, feature) = q
      (individual.property("name").orElse(""),
        variant.property("variantType").orElse(""),
        feature.property("name").orElse(""))
    }
  }
}

