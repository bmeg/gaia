package gaia.test

import gaia.config._
import gaia.graph._
import gaia.ingest.GraphTransform

import scala.io.Source
// import org.json4s._
// import org.json4s.jackson.JsonMethods._

object TestGraph {
  def read(path: String): GaiaGraph = {
    val config = GaiaConfig.readConfig("resources/config/test.yaml")
    val graph = config.connectToGraph(config.graph).get
    val ingestor = GraphTransform(graph)
    //ingestor.ingestFile(path)
    graph

    // Source.fromFile(path).getLines().foreach { line =>
    //   Ingest.ingestVertex(graph) (parse(line))
    // }
  }
}
