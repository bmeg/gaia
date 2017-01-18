package gaia.command

import gaia.api.ingest.FileIngestor
import gaia.config._
import gaia.graph._
import gaia.ingest._
import gaia.server._

import org.rogach.scallop._

import scala.io.Source
import scala.util.Try

object GaiaCommand extends App {
  val defaultConfig = "resources/config/gaia.yaml"

  object Arguments extends ScallopConf(args) {
    val init = new Subcommand("init") {
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

    addSubcommand(ingest)
    addSubcommand(start)
    addSubcommand(init)

    verify()
  }
  
  def loadConfig(path: String): GaiaConfig = {
    GaiaConfig.readConfig(path)
  }

  def connect(path: Option[String]): Try[GaiaGraph] = {
    val configPath = path.getOrElse(defaultConfig)
    val config = loadConfig(configPath)
    config.connectToGraph(config.graph)
  }

  def init() {
    val configPath = Arguments.init.config.toOption.getOrElse(defaultConfig)
    val config = loadConfig(configPath)
    val graph = config.connectToGraph(config.graph)

    if (graph.isSuccess) {
      val migrations = GaiaMigrations.findMigrations(config.graph.migrations.getOrElse(List[String]()))
      GaiaMigrations.registerMigrations(migrations)
      GaiaMigrations.runMigrations(graph.get)
      println("migrations complete!")
    } else {
      println("failed to open graph! " + configPath)
    }

    Runtime.getRuntime.halt(0)
  }

  def ingest() {
    val graph = connect(Arguments.ingest.config.toOption)

    if (graph.isSuccess) {
      val ingestor = new GraphIngestor(graph.get)

      Arguments.ingest.file.toOption match {
        case Some(file) => {
<<<<<<< HEAD
          val ingestor = new GraphTransform(graph.get)
          ingestor.ingestFile(file)
          println("ingested file " + file)
=======
          ingestor.ingestFile(file)
          println("ingested file " + file)
        }
      }

      Arguments.ingest.url.toOption match {
        case Some(url) => {
          ingestor.ingestUrl(url)
          println("ingested url " + url)
>>>>>>> 5812d89b64571348c6364e6190a527c1b3a85d99
        }
      }
    } else {
      println("failed to open graph! " + Arguments.ingest.config.getOrElse(defaultConfig))
    }

    Runtime.getRuntime.halt(0)
  }

  def start() {
    GaiaServer.startServer(Arguments.start.config.toOption.getOrElse(defaultConfig))
  }

  def unknown() {
    println("unknown command!")
  }

  def help() {
    println("help!")
  }

  Arguments.subcommand match {
    case Some(Arguments.init) => init()
    case Some(Arguments.ingest) => ingest()
    case Some(Arguments.start) => start()
    case Some(_) => unknown()
    case None => help()
  }
}
