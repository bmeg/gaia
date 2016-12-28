package gaia.test

import gaia.config._
import gaia.graph._
import gaia.schema._
import gaia.ingest.{GraphTransform, ProtoGrapher}

object TestGraph {
  def read(path: String, protograph: String): GaiaGraph = {
    val config = GaiaConfig.readConfig("resources/config/test.yaml")
    val graph = config.connectToGraph(config.graph).get
    val ingestor = GraphTransform(graph)
    graph
  }
}
