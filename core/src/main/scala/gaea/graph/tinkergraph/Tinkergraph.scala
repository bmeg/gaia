package gaea.graph.tinkergraph

import gaea.config._
import gaea.graph._

import scala.util.Try

import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.apache.commons.configuration.BaseConfiguration

class GaeaTinkergraph(config: GaeaGraphConfig) extends GaeaGraph {
  def connect(): TinkerGraph = {
    TinkerGraph.open()
  }

  lazy val connection = connect()

  def graph(): Graph = {
    connection
  }

  def makeIndex(name: String) (keys: Map[String, Class[_]]): Try[Unit] = {
    Try {}
  }

  def makeIndexes(spec: Map[String, Map[String, Class[_]]]): Try[Unit] = {
    Try {}
  }

  def commit(): Unit = {}
}
