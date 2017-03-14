package gaia.facet

import gaia.graph._
import gaia.transform._
import gaia.collection.Collection._
import gaia.io.JsonIO

import org.json4s._
import org.json4s.jackson._
import org.json4s.jackson.JsonMethods._

import org.http4s._
import org.http4s.server._
import org.http4s.dsl._
import org.http4s.json4s.jackson._

import com.thinkaurelius.titan.core.TitanGraph
import gremlin.scala._
import org.apache.tinkerpop.gremlin.process.traversal.Order
import org.apache.tinkerpop.gremlin.process.traversal.P._

import com.typesafe.scalalogging._
// import _root_.argonaut._, Argonaut._
// import org.http4s.argonaut._
import scalaz.stream.text
import scalaz.stream.Process
import scalaz.stream.Process._
import scalaz.stream.Process1
import scalaz.concurrent.Task
import scala.collection.JavaConversions._

case class MessageFacet(root: String) extends GaiaFacet with LazyLogging {
  implicit val formats = Serialization.formats(NoTypeHints)

  def transformMessage(transform: MessageTransform) (line: String): Task[Unit] = Task {
    val map = JsonIO.readMap(line)
    transform.transform(map)
  }

  def ingestVertex(transform: GraphTransform) (label: String) (line: String): Task[Vertex] = Task {
    val map = JsonIO.readMap(line)
    transform.ingestVertex(label) (map)
  }

  def service(graph: GaiaGraph): HttpService = {
    val transform = GraphTransform(graph)
    HttpService {
      case request @ POST -> Root / "ingest" / label =>
        val ingest = ingestVertex(transform) (label) _
        val vertexes = request.body.pipe(text.utf8Decode).pipe(text.lines(1024 * 1024)).flatMap { line =>
          print(".")
          Process.eval(ingest(line))
        }

        vertexes.runLog.run
        Ok("done!")

      case request @ POST -> Root / "ingest" =>
        val messages = request.bodyAsText.pipe(text.lines(1024 * 1024 * 64)).flatMap { line =>
          Process eval transformMessage(transform) (line)
        }

        messages.runLog.run
        Ok("done!")
    }
  }
}
