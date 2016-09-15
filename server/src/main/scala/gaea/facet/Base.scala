package gaea.facet

import org.http4s._
import org.http4s.server._
import org.http4s.dsl._

object GaeaFacets {
  val facets = List[Tuple2[String, HttpService]](
    ("/vertex/", VertexFacet.service),
    ("/message/", MessageFacet.service) // ,
    // ("/console/", ConsoleFacet.service)
  )
}
