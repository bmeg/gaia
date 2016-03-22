package bmeg.gaea

import org.http4s._
import org.http4s.server._
import org.http4s.dsl._

import _root_.argonaut._, Argonaut._
import org.http4s.argonaut._

import bmeg.gaea.schema.gene.Gene

object HelloWorld {
  val service = HttpService {
    case GET -> Root / "hello" / name =>
      Ok(jSingleObject("message", jString(s"Hello, ${name}")))
    case GET -> Root / "gene" / name =>
      val gene = Gene(name = name)
      Ok(jSingleObject("message", jString(s"Gene ${gene.name}")))
  }
}
