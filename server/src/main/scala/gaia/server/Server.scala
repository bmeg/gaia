package gaia.server

import gaia.config._
import gaia.graph._
import gaia.facet._

import org.http4s._
import org.http4s.server._
import org.http4s.server.blaze.BlazeBuilder

object GaiaServer {
  def envelopPath(path: String): String = {
    val prefix = if (path.charAt(0) != '/') '/' else ""
    val suffix = if (path.charAt(path.length - 1) != '/') '/' else ""
    prefix + path + suffix
  }

  def findFacet(className: String) (path: String): GaiaFacet = {
    val qualifiedName = if (!className.contains(".")) className else "gaia.facet." + className
    val constructor = Class.forName(qualifiedName).getConstructor(classOf[String])
    val enveloped = envelopPath(path)
    constructor.newInstance(enveloped).asInstanceOf[GaiaFacet]
  }

  def findFacets(facetConfig: Map[String, String]): List[GaiaFacet] = {
    facetConfig.map(facet => findFacet(facet._1) (facet._2)).toList
  }

  def start(config: GaiaServerConfig) (graph: GaiaGraph): Unit = {
    val facets = GaiaServer.findFacets(config.facets.getOrElse(Map[String, String]()))
    val blaze = BlazeBuilder.bindHttp(config.port.getOrElse(11223))
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
    GaiaServer.start(config.server) (graph.get)
  } else {
    println("failed to connect to graph: " + config.graph.toString)
  }
}
