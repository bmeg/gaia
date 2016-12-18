package gaia.command

import gaia.api.ingest.FileIngestor
import gaia.config._
import gaia.graph._
import gaia.ingest.GraphIngestor
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
  
  def connect(path: ScallopOption[String]): Try[GaiaGraph] = {
    val configPath = path.getOrElse(defaultConfig)
    val config = GaiaConfig.readConfig(configPath)
    config.connectToGraph(config.graph)
  }

  def migrate() {
    val graph = connect(Arguments.migrate.config)

    if (graph.isSuccess) {
      GaiaMigrations.runMigrations(graph.get)
      println("migrations complete!")
    } else {
      println("failed to open graph! " + Arguments.migrate.config.getOrElse(defaultConfig))
    }

    Runtime.getRuntime.halt(0)
  }

  def ingest() {
    val graph = connect(Arguments.ingest.config)

    if (graph.isSuccess) {
      if (Arguments.ingest.file != None) {
        val fing = new GraphIngestor(graph.get)
        Source.fromFile(Arguments.ingest.file.toOption.get).getLines().foreach(x => {
          fing.ingestMessage(x)
        })
      }
    } else {
      println("failed to open graph! " + Arguments.ingest.config.getOrElse(defaultConfig))
    }

    Runtime.getRuntime.halt(0)
  }

  def start() {
    println("start: " + Arguments.start.config)
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
