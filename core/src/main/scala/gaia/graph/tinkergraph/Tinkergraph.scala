package gaia.graph.tinkergraph

import gaia.config._
import gaia.graph._
import gaia.schema._

import scala.util.Try

import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.apache.commons.configuration.BaseConfiguration

class GaiaTinkergraph(config: GaiaGraphConfig) extends GaiaGraph {
  def connect(): TinkerGraph = {
    TinkerGraph.open()
  }

  lazy val connection = connect()
  def graph: Graph = {
    connection
  }

  lazy val persistedSchema: GaiaSchema = GaiaSchema.load(config.protograph.getOrElse("default"))
  def schema: GaiaSchema = {
    persistedSchema
  }

  // lazy val persistedSchema = GraphSchema.assemble(List[GaiaVertex](), List[GaiaEdge]())
  // def schema: GraphSchema = {
  //   persistedSchema
  // }

  def makeIndex(name: String) (keys: Map[String, Class[_]]): Try[Unit] = {
    Try {}
  }

  def makeIndexes(spec: Map[String, Map[String, Class[_]]]): Try[Unit] = {
    Try {}
  }

  def commit(): Unit = {}
}
