package bmeg.gaea.titan

import bmeg.gaea.convoy.Convoy
import bmeg.gaea.convoy.Hugo

import com.thinkaurelius.titan.core.TitanGraph

object TitanMigration {
  def migrate(): TitanGraph = {
    val config = Titan.configuration(Map[String, String]())
    val graph = Titan.connect(config)
    Titan.makeIndexes(graph) (Convoy.indexSpec)
    Hugo.hugoMigration(graph) ("resources/hugo-names")
    graph
  }
}
