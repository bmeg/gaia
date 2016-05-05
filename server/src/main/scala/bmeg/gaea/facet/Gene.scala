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
import scalaz.stream.Process
import scalaz.stream.Process._
import scalaz.stream.Process1

object GeneFacet {
  val graph = Titan.connect(Titan.configuration())

  def splitLines(rest: String): Process1[String, String] =
    rest.split("""\r\n|\n|\r""", 2) match {
      case Array(head, tail) =>
        emit(head) ++ splitLines(tail)
      case Array(head) =>
        receive1Or[String, String](emit(rest)) { s => splitLines(rest + s) }
    }


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
        val individualList = Convoy.parseIndividualList(raw)
        val size = Convoy.ingestIndividualList(individualList)
        Ok(jNumber(size))
      }

    case request @ POST -> Root / "individuals" =>
      // val individuals = request.bodyAsText.pipe(splitLines(_: String)).map { line =>
      //   Convoy.parseIndividual(line)
      // }

      // Convoy.ingestIndividuals(individuals)
      request.as[String].flatMap { raw =>
        println(raw.size)
        val lines = raw.split("\n")
        val individuals = lines.map(Convoy.parseIndividual(_))
        Convoy.ingestIndividuals(individuals.toList)
        Ok(jNumber(individuals.size))
      }

    case request @ POST -> Root / "yellow" =>
      var x = 0
      val y = request.bodyAsText.map { line =>
        x = x+1
        println(line)
        Process(x)
      }
      y.run
      Ok(jNumber(x))
  }
}
