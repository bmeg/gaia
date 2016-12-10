package gaia.command

import gaia.config._
import gaia.graph._

import org.rogach.scallop._

object GaiaCommand extends App {
  println(args.getClass)
  println(args.toString)

  object Arguments extends ScallopConf(args) {
    val migrate = new Subcommand("migrate") {
      val config = trailArg[String]()
    }

    val ingest = new Subcommand("ingest") {
      val config = trailArg[String]()
    }

    val start = new Subcommand("start") {
      val config = trailArg[String]()
    }

    addSubcommand(migrate)
    addSubcommand(ingest)
    addSubcommand(start)

    verify()
  }
  
  def migrate() {
    println("migrate: " + Arguments.migrate.config)
  }

  def ingest() {
    println("ingest: " + Arguments.ingest.config)
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

  val config = GaiaConfig.readConfig("resources/config/gaia.yaml")
  val graph = config.connectToGraph(config.graph)
  if (graph.isSuccess) {
    println("success!")
  } else {
    println("failed to connect to graph: " + config.graph.toString)
  }
}
