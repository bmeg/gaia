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

case class SchemaFacet(root: String) extends GaiaFacet with LazyLogging {
  implicit val formats = Serialization.formats(NoTypeHints)

  def service(graph: GaiaGraph): HttpService = {
    HttpService {
      case GET -> Root / "protograph" =>
        val structure = graph.schema.protograph.graphStructure
        Ok(Extraction.decompose(structure))
    }
  }
}
