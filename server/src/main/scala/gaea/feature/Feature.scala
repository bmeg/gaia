package gaea.feature

import com.thinkaurelius.titan.core.TitanGraph
import gremlin.scala._
import gaea.collection.Collection._
import org.apache.tinkerpop.gremlin.process.traversal.P._

object Feature {
  val Name = Key[String]("name")
  val synonymPrefix = "feature:"

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
}

