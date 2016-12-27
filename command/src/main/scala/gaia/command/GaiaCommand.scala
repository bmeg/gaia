package gaia.command

import gaia.api.ingest.FileIngestor
import gaia.config._
import gaia.graph._
import gaia.ingest._

import org.rogach.scallop._

import scala.io.Source
import scala.util.Try

object GaiaCommand extends App {
  val defaultConfig = "resources/config/gaia.yaml"

  object Arguments extends ScallopConf(args) {
    val migrate = new Subcommand("migrate") {
      val config = opt[String]("config", required=false)
    }

    val ingest = new Subcommand("ingest") {
      val config = opt[String]("config", required=false)
      val kafka = opt[String]("kafka", required=false)
      val file = opt[String]("file", required=false)
      val url = opt[String]("url", required=false)
    }

    val start = new Subcommand("start") {
      val config = opt[String]("config", required=false)
    }

    addSubcommand(migrate)
    addSubcommand(ingest)
    addSubcommand(start)

    verify()
  }
  
  def connect(path: Option[String]): Try[GaiaGraph] = {
    val configPath = path.getOrElse(defaultConfig)
    val config = GaiaConfig.readConfig(configPath)
    config.connectToGraph(config.graph)
  }

  def migrate() {
    val graph = connect(Arguments.migrate.config.toOption)

    if (graph.isSuccess) {
      GaiaMigrations.runMigrations(graph.get)
      println("migrations complete!")
    } else {
      println("failed to open graph! " + Arguments.migrate.config.getOrElse(defaultConfig))
    }

    Runtime.getRuntime.halt(0)
  }

  def ingest() {
    val graph = connect(Arguments.ingest.config.toOption)

    if (graph.isSuccess) {
      Arguments.ingest.file.toOption match {
        case Some(file) => {
          val ingestor = new GraphTransform(graph.get)
          ingestor.ingestFile(file)
          println("ingested file " + file)
        }
      }
    } else {
      println("failed to open graph! " + Arguments.ingest.config.getOrElse(defaultConfig))
    }

    Runtime.getRuntime.halt(0)
  }

  def start() {
    println("start: " + Arguments.start.config.toOption)
  }

  def unknown() {
    println("unknown command!")
  }

  def help() {
    println("help!")
  }

  Arguments.subcommand match {
    case Some(Arguments.migrate) => migrate()
    case Some(Arguments.ingest) => ingest()
    case Some(Arguments.start) => start()
    case Some(_) => unknown()
    case None => help()
  }
}
