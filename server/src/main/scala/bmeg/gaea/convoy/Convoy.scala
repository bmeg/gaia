package bmeg.gaea.convoy

import bmeg.gaea.titan.Titan
import bmeg.gaea.schema.Variant

import com.thinkaurelius.titan.core.TitanGraph
import gremlin.scala._
import scala.collection.JavaConverters._

object Convoy {
  val idKey = Key[String]("id")
  val nameKey = Key[String]("name")
  val sourceKey = Key[String]("source")

  def ingestVariantCall(graph: TitanGraph) (bioSampleVertex: Vertex) (variantCall: Variant.VariantCall): Vertex = {
    val variantCallVertex = Titan.findVertex(graph) ("variantCall") (idKey) (variantCall.getId()).getOrElse {
      graph + ("variantCall",
        idKey -> variantCall.getId()
      )
    }

    variantCallVertex
  }

  def ingestBioSample(graph: TitanGraph) (individualVertex: Vertex) (bioSample: Variant.BioSample): Vertex = {
    val bioSampleVertex = Titan.findVertex(graph) ("bioSample") (nameKey) (bioSample.getName()).getOrElse {
      graph + ("bioSample",
        idKey -> bioSample.getId(),
        nameKey -> bioSample.getName()
      )
    }

    val variantCalls = bioSample.getVariantCallsList().asScala.toList
    val variantCallVertexes = variantCalls.map(ingestVariantCall(graph) (bioSampleVertex))
    bioSampleVertex
  }

  def ingestIndividual(graph: TitanGraph) (individual: Variant.Individual): Vertex = {
    val individualVertex = Titan.findVertex(graph) ("individual") (nameKey) (individual.getName()).getOrElse {
      graph + ("individual",
        idKey -> individual.getId(),
        nameKey -> individual.getName(),
        sourceKey -> individual.getSource()
      )
    }

    val bioSamples = individual.getBioSamplesList().asScala.toList
    val bioSampleVertexes = bioSamples.map(ingestBioSample(graph) (individualVertex))
    individualVertex
  }

  def ingestIndividualList(individualList: Variant.IndividualList): Int = {
    val individuals = individualList.getIndividualsList().asScala.toList
    val graph = Titan.connect(Titan.configuration())
    println(s"Ingesting ${individuals.length} individuals")

    val individualVertexes = individuals.map(ingestIndividual(graph))
    graph.tx.commit()
    individualVertexes.length
  }
}
