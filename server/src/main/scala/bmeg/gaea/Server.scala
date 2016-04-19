package bmeg.gaea

import bmeg.gaea.facet.GeneFacet
import org.http4s.server.blaze.BlazeBuilder

object GaeaFoundation extends App {
  BlazeBuilder.bindHttp(11223)
    .mountService(GeneFacet.service, "/")
    .run
    .awaitShutdown()
}
