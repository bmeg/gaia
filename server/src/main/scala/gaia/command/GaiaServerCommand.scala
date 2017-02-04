package gaia.command

import gaia.config._
import gaia.graph._
import gaia.server._

import scala.io.Source
import scala.util.Try

class GaiaServerCommand(release: String) extends GaiaCommand(release) {
  override def parser = {
    val inner = super.parser
    val config = new scopt.OptionParser[Map[String, String]] ("config") {
      opt[String]("config")
        .action((x, m) => m + ("config" -> x))
        .text("path to gaia config file")
    }

    inner.cmd("start")
      .action((_, m) => m + ("command" -> "start"))
      .text("start the gaia http and kafka servers")
      .children(
        inner.opt[String]("config")
          .action((x, m) => m + ("config" -> x))
          .text("path to gaia config file"))

    inner
  }

  override def interpret(command: Map[String, String]) {
    command.get("command") match {
      case Some("start") => start(command)
      case _ => super.interpret(command)
    }
  }

  def start(command: Map[String, String]) {
    GaiaServer.startServer(command.get("config").get)
  }
}
