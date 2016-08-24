package gaea

import gaea.facet.GeneFacet
import scala.concurrent.duration._
import org.http4s.server.blaze.BlazeBuilder

object GaeaFoundation extends App {
  BlazeBuilder
    .withIdleTimeout(60.seconds)
    .bindHttp(11223)
    .mountService(GeneFacet.service, "/")
    .run
    .awaitShutdown()
}
