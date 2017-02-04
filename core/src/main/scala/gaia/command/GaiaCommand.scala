package gaia.command

import gaia.config._
import gaia.graph._
import gaia.transform._
import gaia.ingest._

import scala.io.Source
import scala.util.Try

class GaiaCommand(release: String) {
  val default = Map("command" -> "help", "config" -> "resources/config/gaia.yaml")
  
  def parser = new scopt.OptionParser[Map[String, String]] ("gaia") {
    head("gaia", release)

    cmd("help")
      .action((_, m) => m + ("command" -> "help"))
      .text("display help menu")

    cmd("init")
      .action((_, m) => m + ("command" -> "init"))
      .text("prepare a gaia instance for use")
      .children(
        opt[String]("config")
          .action((x, m) => m + ("config" -> x))
          .text("path to gaia config file"))

    cmd("ingest")
      .action((_, m) => m + ("command" -> "ingest"))
      .text("ingest vertexes and edges from an external source")
      .children(
        opt[String]("config")
          .action((x, m) => m + ("config" -> x))
          .text("path to gaia config file"),
        opt[String]("label")
          .action((x, m) => m + ("label" -> x))
          .text("path to gaia config file"),
        opt[String]("kafka")
          .action((x, m) => m + ("kafka" -> x))
          .text("host and port of kafka server"),
        opt[String]("file")
          .action((x, m) => m + ("file" -> x))
          .text("path to a file containing protograph messages"),
        opt[String]("dir")
          .action((x, m) => m + ("dir" -> x))
          .text("path to a directory containing protograph message files"),
        opt[String]("url")
          .action((x, m) => m + ("url" -> x))
          .text("url of a file containing protograph messages")
      )
  }

  def parseOptions(args: Seq[String]): Map[String, String] = {
    parser.parse(args, default).getOrElse(default)
  }

  def help(command: Map[String, String]) {
    println("helpful")
  }

  def init(command: Map[String, String]) {
    val config = GaiaConfig.readConfig(command.get("config").get)
    GaiaMigrations.migrate(config)
  }

  def ingest(command: Map[String, String]) {
    val config = GaiaConfig.readConfig(command.get("config").get)
    val graph = config.connectToGraph(config.graph)

    if (graph.isSuccess) {
      val ingestor = new GraphTransform(graph.get)

      command.get("file").map { (file) =>
        val label = command.get("label").getOrElse(ingestor.findLabel(file))
        ingestor.ingestPath(label, file)
        println("ingested file " + file + " as " + label)
      }

      command.get("url").map { (url) =>
        val label = command.get("label").getOrElse(ingestor.findLabel(url))
        ingestor.ingestUrl(label, url)
        println("ingested url " + url + " as " + label)
      }
    } else {
      println("failed to open graph! " + config)
    }
  }

  def interpret(command: Map[String, String]) {
    command.get("command") match {
      case Some("help") => help(command)
      case Some("init") => init(command)
      case Some("ingest") => ingest(command)
      case None => help(default)
    }
  }

  def execute(args: Seq[String]) {
    val command = parseOptions(args)
    interpret(command)
  }
}

object GaiaCommand extends App {
  val parser = new GaiaCommand("0.0.7")
  parser.execute(args)
}
