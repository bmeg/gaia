package gaea.server

import gaea.facet.GaeaFacets
import gaea.facet.StaticFacet

import org.http4s._
import org.http4s.server._
import org.http4s.server.blaze.BlazeBuilder

object GaeaServer {
  def start(facets: Seq[Tuple2[String, HttpService]]): Unit = {
    val blaze = BlazeBuilder.bindHttp(11223)
    val mounted = (GaeaFacets.facets ++ facets).foldLeft(blaze) { (blaze, facet) =>
      blaze.mountService(facet._2, "/gaea" + facet._1)
    }

    val static = mounted.mountService(StaticFacet.service, "/")
    static.run.awaitShutdown()
  }
}
