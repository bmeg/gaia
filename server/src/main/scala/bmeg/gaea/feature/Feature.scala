package bmeg.gaea.feature

import com.thinkaurelius.titan.core.TitanGraph
import gremlin.scala._

object Feature {
  val symbolKey = Key[String]("symbol")

  def findSynonymVertex(graph: TitanGraph) (symbol: String): Option[Vertex] = {
    graph.V.hasLabel("featureSynonym").has(symbolKey, symbol).out("synonymFor").headOption
  }

  def findSynonym(graph: TitanGraph) (symbol: String): Option[String] = {
    val values = findSynonymVertex(graph) (symbol).map(_.valueMap())
    values.map(_("symbol").asInstanceOf[String])
  }

  def findFeature(graph: TitanGraph) (symbol: String): Vertex = {
    findSynonymVertex(graph) (symbol).getOrElse {
      val synonym = graph.V.hasLabel("featureSynonym").has(symbolKey, symbol).headOption.getOrElse {
        graph + ("featureSynonym", symbolKey -> symbol)
      }

      val feature = graph.V.hasLabel("feature").has(symbolKey, symbol).headOption.getOrElse {
        graph + ("feature", symbolKey -> symbol)
      }

      synonym --- ("synonymFor") --> feature
      feature
    }
  }
}
