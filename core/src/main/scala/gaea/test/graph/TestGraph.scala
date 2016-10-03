package gaea.test

import gaea.config._
import gaea.graph._
import gaea.ingest.Ingest

import scala.io.Source
import org.json4s._
import org.json4s.jackson.JsonMethods._

object TestGraph {
  def read(path: String): GaeaGraph = {
    val config = GaeaConfig.readConfig("resources/config/test.yaml")
    val graph = config.connectToGraph(config.graph).get

    Source.fromFile(path).getLines().foreach { line =>
      Ingest.ingestVertex(graph) (parse(line))
    }

    graph
  }
}
