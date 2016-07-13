package gaea.facet

import gaea.titan.Titan
import gaea.ingest.Ingest
import gaea.collection.Collection._

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

import scala.collection.JavaConversions._

object VertexFacet extends LazyLogging {
  lazy val graph = Titan.connection
  val Name = Key[String]("name")

  def mapToJson(properties: Map[String, Any]) : Json = {
    properties.map( x => {
      ((x._1), (x._2.toString))
    } ).asJson
  }

  def countVertexes(graph: TitanGraph): Map[String, Long] = {
    val counts = graph.V.traversal.label.groupCount.toList.get(0)
    val labels = counts.keySet.toList.asInstanceOf[List[String]]
    labels.foldLeft(Map[String, Long]()) { (countMap, label) =>
      countMap + (label.toString -> counts.get(label))
    }
  }

  lazy val vertexCounts = countVertexes(graph)

  val service = HttpService {
    case GET -> Root / "counts" =>
      Ok(vertexCounts.asJson)

    case GET -> Root / "find" / name =>
      val vertex = graph.V.has(Name, name).head
      val o = vertex.out().value(Name).toList()
      val i = vertex.in().value(Name).toList()
      val out = Map[String,Json](
        "type" -> vertex.label().asJson,
        "properties" -> mapToJson(vertex.valueMap),
        "out" -> o.asJson,
        "in" -> i.asJson
      )
      Ok(out.asJson)
  }
}
