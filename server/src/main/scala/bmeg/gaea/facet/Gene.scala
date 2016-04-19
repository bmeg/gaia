package bmeg.gaea.facet

import bmeg.gaea.titan.Titan
import bmeg.gaea.schema.Variant
import bmeg.gaea.convoy.Convoy
import bmeg.gaea.feature.Feature

import org.http4s._
import org.http4s.server._
import org.http4s.dsl._

import _root_.argonaut._, Argonaut._
import org.http4s.argonaut._
import com.google.protobuf.util.JsonFormat

object GeneFacet {
  val graph = Titan.connect(Titan.configuration())

  val service = HttpService {
    case GET -> Root / "hello" / name =>
      Ok(jSingleObject("message", jString(s"Hello, ${name}")))

    case GET -> Root / "gene" / name =>
      val synonym = Feature.findSynonym(graph) (name).getOrElse {
        "no synonym found"
      }
      Ok(jSingleObject(name, jString(synonym)))

    case request @ POST -> Root / "individual-list" =>
      request.as[String].flatMap { raw =>
        val individualList: Variant.IndividualList.Builder = Variant.IndividualList.newBuilder()
        JsonFormat.parser().merge(raw, individualList)
        val size = Convoy.ingestIndividualList(individualList.build())
        Ok(jNumber(size))
      }
  }
}
