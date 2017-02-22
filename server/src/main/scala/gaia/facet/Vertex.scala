package gaia.facet

import gaia.graph.GaiaGraph
import gaia.collection.Collection._
import gaia.html.VertexHtml
import gaia.query._

import org.json4s._
import org.json4s.jackson._
import org.json4s.jackson.JsonMethods._

import org.http4s._
import org.http4s.server._
import org.http4s.dsl._
import org.http4s.MediaType._
import org.http4s.headers.{`Content-Type`, `Content-Length`}
import org.http4s.json4s.jackson._

import shapeless._
import gremlin.scala._
import org.apache.tinkerpop.gremlin.process.traversal.Order
import org.apache.tinkerpop.gremlin.process.traversal.P._

import com.typesafe.scalalogging._
import scala.collection.JavaConversions._
import scala.collection.mutable

case class VertexFacet(root: String) extends GaiaFacet with LazyLogging {
  val Gid = Key[String]("gid")
  val queries = mutable.Map[String, String]()

  implicit val formats = Serialization.formats(NoTypeHints)

  def mapToJson(properties: Map[String, Any]) : JValue = {
    Extraction.decompose(properties.map( x => {
      ((x._1), (x._2.toString))
    }))
  }

  def countVertexes(graph: GaiaGraph): Map[String, Long] = {
    val counts = graph.V.traversal.label.groupCount.toList.get(0)
    val labels = counts.keySet.toList.asInstanceOf[List[String]]
    labels.foldLeft(Map[String, Long]()) { (countMap, label) =>
      countMap + (label.toString -> counts.get(label))
    }
  }

  def service(graph: GaiaGraph): HttpService = {
    HttpService {
      // case GET -> Root / "counts" =>
      //   Ok(vertexCounts.asJson)

      case GET -> Root / "explore" =>
        Ok(VertexHtml.layout(VertexHtml.vertex).toString)
          .withContentType(Some(`Content-Type`(`text/html`)))

      case GET -> Root / "find" / gid =>
        try {
          val vertex = graph.V.has(Gid, gid).head
          val inEdges = groupAs[Edge, String, String](vertex.inE.toList) (_.label) (_.outVertex.value[String]("gid"))
          val outEdges = groupAs[Edge, String, String](vertex.outE.toList) (_.label) (_.inVertex.value[String]("gid"))

          val out = Map[String, JValue](
            "type" -> Extraction.decompose(vertex.label),
            "properties" -> mapToJson(vertex.valueMap),
            "in" -> Extraction.decompose(inEdges),
            "out" -> Extraction.decompose(outEdges)
          )

          Ok(Extraction.decompose(out))
        }

        catch {
          case _: Throwable => Ok(Extraction.decompose(Map[String, JValue]()))
        }

      case request @ POST -> Root / "query" =>
        request.as[String].flatMap { raw =>
          println(raw)

          val json = queries.get(raw).getOrElse {
            val query = GaiaQuery.parse(raw)
            val result = query.executeJson(graph)
            queries(raw) = result
            result
          }

          Ok(json)
        }
    }
  }
}
