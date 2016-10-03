package gaea.server

import gaea.config._
import gaea.graph._
import gaea.facet._

import org.http4s._
import org.http4s.server._
import org.http4s.server.blaze.BlazeBuilder

object GaeaServer {
  def start(config: GaeaServerConfig) (graph: GaeaGraph) (facets: Seq[GaeaFacet]): Unit = {
    val blaze = BlazeBuilder.bindHttp(config.port)
    val mounted = (BaseFacets.facets ++ facets).foldLeft(blaze) { (blaze, facet) =>
      blaze.mountService(facet.service(graph), "/gaea" + facet.root)
    }

    val static = mounted.mountService(StaticFacet.service, "/")
    static.run.awaitShutdown()
  }
}

object GaeaFoundation extends App {
  val config = GaeaConfig.readConfig("resources/config/gaea.yaml")
  val graph = config.connectToGraph(config.graph)
  if (graph.isSuccess) {
    GaeaServer.start(config.server) (graph.get) (List[GaeaFacet]())
  } else {
    println("failed to connect to graph: " + config.graph.toString)
  }
}
