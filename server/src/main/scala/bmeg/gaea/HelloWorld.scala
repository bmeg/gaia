package bmeg.gaea

import org.http4s._
import org.http4s.server._
import org.http4s.dsl._

import _root_.argonaut._, Argonaut._
import org.http4s.argonaut._

import com.trueaccord.scalapb.json.JsonFormat

import bmeg.gaea.schema.gene.Gene
import bmeg.gaea.schema.variant._
import bmeg.gaea.titan.Titan

object HelloWorld {
  val service = HttpService {

    case GET -> Root / "hello" / name =>
      Ok(jSingleObject("message", jString(s"Hello, ${name}")))

    case GET -> Root / "gene" / name =>
      val graph = Titan.connect(Titan.configuration())
      val gene = Gene(name = name)
      Ok(jSingleObject("message", jString(s"Gene ${gene.name}")))

    case request @ POST -> Root / "gene-test" =>
      request.as[String].flatMap { raw =>
        val gene: Gene = JsonFormat.fromJsonString[Gene](raw)
        val geneName:String = gene.name
        val yellow: Gene = gene.update(_.name := geneName + "yellow")
        val format: String = JsonFormat.toJsonString(yellow)
        val response: Option[Json] = Parse.parseOption(format)
        Ok(response.getOrElse(Json()))
      }

    case request @ POST -> Root / "individual-list" =>
      request.as[String].flatMap { raw =>
        val data: IndividualList = JsonFormat.fromJsonString[IndividualList](raw)
        val individuals: Seq[Individual] = data.individuals
        Ok(jNumber(individuals.length))
      }
  }
}
