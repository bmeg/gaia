package gaea.server

import gaea.titan.Titan
import gaea.feature.Feature

import org.http4s._
import org.http4s.server._
import org.http4s.dsl._

import _root_.argonaut._, Argonaut._
import org.http4s.argonaut._
import com.google.protobuf.util.JsonFormat

// import scodec.bits.ByteVector
// import com.trueaccord.scalapb.json.JsonFormat

object HelloWorld {
  val graph = Titan.connect(Titan.configuration(Map[String, String]()))

  val service = HttpService {
    case GET -> Root / "hello" / name =>
      Ok(jSingleObject("message", jString(s"Hello, ${name}")))

    case GET -> Root / "gene" / name =>
      val synonym = Feature.findSynonym(graph) (name).getOrElse {
        "no synonym found"
      }
      Ok(jSingleObject(name, jString(synonym)))

    // case request @ POST -> Root / "individual-list" =>
    //   request.as[String].flatMap { raw =>
    //     val individualList: Variant.IndividualList.Builder = Variant.IndividualList.newBuilder()
    //     JsonFormat.parser().merge(raw, individualList)
    //     val size = Convoy.ingestIndividualList(individualList.build())
    //     Ok(jNumber(size))
    //   }

    // case request @ POST -> Root / "individual-list" =>
    //   request.as[String].flatMap { raw =>
    //     val individualList = ProtobufferMessage.parse(raw, Variant.IndividualList.newBuilder())
    //     val individuals = individualList.getIndividualsList()
    //     Ok(jNumber(individuals.length))
    //   }

    // case request @ POST -> Root / "gene-test" =>
    //   request.as[String].flatMap { raw =>
    //     val gene: Gene = JsonFormat.fromJsonString[Gene](raw)
    //     val geneName: String = gene.name
    //     val yellow: Gene = gene.update(_.name := geneName + "yellow")
    //     val format: String = JsonFormat.toJsonString(yellow)
    //     val response: Option[Json] = Parse.parseOption(format)
    //     Ok(response.getOrElse(Json()))
    //   }

    // case request @ POST -> Root / "individual-list-json" =>
    //   request.as[String].flatMap { raw =>
    //     val data: IndividualList = JsonFormat.fromJsonString[IndividualList](raw)
    //     val individuals: Seq[Individual] = data.individuals
    //     Ok(jNumber(individuals.length))
    //   }

    // case request @ POST -> Root / "individual-list" =>
    //   request.as[ByteVector].flatMap { raw =>
    //     val data = Variant.IndividualList.parseFrom(raw.toArray)
    //     val individuals = data.getIndividualsList()
    //     Ok(jNumber(individuals.length))
    //   }

  }
}
