package gaia.server

import gaia.config._
import gaia.graph._
import gaia.facet._

import org.http4s._
import org.http4s.server._
import org.http4s.server.blaze.BlazeBuilder

object GaiaServer {
  def start(config: GaiaServerConfig) (graph: GaiaGraph) (facets: Seq[GaiaFacet]): Unit = {
    val blaze = BlazeBuilder.bindHttp(config.port)
    val mounted = (BaseFacets.facets ++ facets).foldLeft(blaze) { (blaze, facet) =>
      blaze.mountService(facet.service(graph), "/gaia" + facet.root)
    }

    val static = mounted.mountService(StaticFacet.service, "/")
    static.run.awaitShutdown()
  }
}

object GaiaFoundation extends App {
  val config = GaiaConfig.readConfig("resources/config/gaia.yaml")
  val graph = config.connectToGraph(config.graph)
  if (graph.isSuccess) {
    GaiaServer.start(config.server) (graph.get) (List[GaiaFacet]())
  } else {
    println("failed to connect to graph: " + config.graph.toString)
  }
}
