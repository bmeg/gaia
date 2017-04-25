package gaia.test

import gaia.config._
import gaia.graph._
import gaia.schema._
import gaia.transform._

object TestGraph {
  def read(path: String): GaiaGraph = {
    val config = GaiaConfig.readConfig("resources/config/test.yaml")
    val graph = config.connectToGraph(config.graph).get
    val transform = GraphTransform(graph)
    graph
  }
}
