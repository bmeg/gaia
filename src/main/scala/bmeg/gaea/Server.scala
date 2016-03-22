package bmeg.gaea

import org.http4s.server.blaze.BlazeBuilder

object GaeaFoundation extends App {
  BlazeBuilder.bindHttp(11223)
    .mountService(HelloWorld.service, "/")
    .run
    .awaitShutdown()
}
