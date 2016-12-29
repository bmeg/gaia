package gaia.config

import gaia.graph._
import gaia.graph.titan._
import gaia.graph.tinkergraph._

import scala.util.Try
import scala.io.Source

import net.jcazevedo.moultingyaml._
import net.jcazevedo.moultingyaml.DefaultYamlProtocol._

case class GaiaGraphConfig(
  database: Option[String],
  host: Option[String],
  keyspace: Option[String]
)

case class GaiaServerConfig(
  port: Option[Int],
  facets: Option[Map[String, String]]
)

case class GaiaConfig(graph: GaiaGraphConfig, server: GaiaServerConfig) {
  def connectToGraph(config: GaiaGraphConfig): Try[GaiaGraph] = {
    Try {
      val database = config.database.getOrElse("tinkergraph")
      if (database == "tinkergraph") {
        new GaiaTinkergraph(config)
      } else if (database == "titan") {
        new GaiaTitan(config)
      } else {
        throw new Exception("database not supported")
      }
    }
  }
}

object GaiaConfigProtocol extends DefaultYamlProtocol {
  implicit val graphFormat = yamlFormat3(GaiaGraphConfig.apply)
  implicit val serverFormat = yamlFormat2(GaiaServerConfig.apply)
  implicit val configFormat = yamlFormat2(GaiaConfig.apply)
}

import GaiaConfigProtocol._

object GaiaConfig {
  def readConfig(path: String): GaiaConfig = {
    val raw = Source.fromFile(path).getLines.mkString("\n")
    val yaml = raw.parseYaml
    yaml.convertTo[GaiaConfig]
  }

  def defaultGraph(): GaiaGraph = {
    val config = GaiaConfig.readConfig("resources/config/gaia.yaml")
    config.connectToGraph(config.graph).get
  }
}
