package gaea.config

import gaea.graph._
import gaea.graph.titan._
import gaea.graph.tinkergraph._

import scala.util.Try
import scala.io.Source

import net.jcazevedo.moultingyaml._
import net.jcazevedo.moultingyaml.DefaultYamlProtocol._

case class GaeaGraphConfig(
  database: String = "tinkergraph",
  host: String = "localhost",
  keyspace: String = "gaea"
) {}

case class GaeaServerConfig(
  port: Int = 11223
) {}

case class GaeaConfig(graph: GaeaGraphConfig, server: GaeaServerConfig) {
  def connectToGraph(config: GaeaGraphConfig): Try[GaeaGraph] = {
    Try {
      val database = config.database
      if (database == "tinkergraph") {
        new GaeaTinkergraph(config)
      } else if (database == "titan") {
        new GaeaTitan(config)
      } else {
        throw new Exception("database not supported")
      }
    }
  }
}

object GaeaConfigProtocol extends DefaultYamlProtocol {
  implicit val graphFormat = yamlFormat3(GaeaGraphConfig.apply)
  implicit val serverFormat = yamlFormat1(GaeaServerConfig.apply)
  implicit val configFormat = yamlFormat2(GaeaConfig.apply)
}

import GaeaConfigProtocol._

object GaeaConfig {
  def readConfig(path: String): GaeaConfig = {
    val raw = Source.fromFile(path).getLines.mkString("\n")
    val yaml = raw.parseYaml
    yaml.convertTo[GaeaConfig]
  }
}
