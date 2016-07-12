package gaea.facet

import gaea.titan.Titan
import gaea.titan.Console

import org.http4s._
import org.http4s.server._
import org.http4s.dsl._

import com.thinkaurelius.titan.core.TitanGraph
import gremlin.scala._
import org.apache.tinkerpop.gremlin.process.traversal.Order
import org.apache.tinkerpop.gremlin.process.traversal.P._

import com.typesafe.scalalogging._
import _root_.argonaut._, Argonaut._
import org.http4s.argonaut._

object ConsoleFacet extends LazyLogging {
  val service = HttpService {
    case request @ POST -> Root / "query" =>
      request.as[Json].flatMap { query =>
        val queryLens = jObjectPL >=> jsonObjectPL("query") >=> jStringPL
        val line = queryLens.get(query).getOrElse("")

        println(line)

        val result = Console.interpret[Any](line) match {
          case Right(result) => result
          case Left(error) => error
        }

        // val result = try {
        //   Console.interpret[Any](line).toString
        // } catch {
        //   case e: Throwable => println(e.getCause); println(e.printStackTrace); e.getMessage();
        //     // .replaceAll("scala.tools.reflect.ToolBoxError: reflective compilation has failed:", "")
        // }

        println(result)

        Ok(("result" -> jString(result.toString)) ->: jEmptyObject)
      }
  }
}
