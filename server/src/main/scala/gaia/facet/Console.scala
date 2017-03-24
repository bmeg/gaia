package gaia.facet

import gaia.graph._
import gaia.eval.Console

import org.json4s._
import org.json4s.jackson._
import org.json4s.jackson.JsonMethods._

import org.http4s._
import org.http4s.server._
import org.http4s.dsl._
import org.http4s.json4s.jackson._

import gremlin.scala._
// import com.thinkaurelius.titan.core.TitanGraph
import org.apache.tinkerpop.gremlin.process.traversal.Order
import org.apache.tinkerpop.gremlin.process.traversal.P._

import com.typesafe.scalalogging._
// import _root_.argonaut._, Argonaut._
// import org.http4s.argonaut._

case class ConsoleFacet(root: String) extends GaiaFacet with LazyLogging {
  def service(graph: GaiaGraph): HttpService = {
    val console = new Console(graph)

    HttpService {
      case request @ POST -> Root / "query" =>
        request.as[JValue].flatMap { query =>
          // val queryLens = jObjectPL >=> jsonObjectPL("query") >=> jStringPL
          // val line = queryLens.get(query).getOrElse("")

          // println(line)

          // val result = console.interpret[Any](line) match {
          //   case Right(result) => result
          //   case Left(error) => error
          // }

          // // val result = try {
          // //   Console.interpret[Any](line).toString
          // // } catch {
          // //   case e: Throwable => println(e.getCause); println(e.printStackTrace); e.getMessage();
          // //     // .replaceAll("scala.tools.reflect.ToolBoxError: reflective compilation has failed:", "")
          // // }

          // println(result)

          // Ok(("result" -> jString(result.toString)) ->: jEmptyObject)
          Ok("what")
        }
    }
  }
}
