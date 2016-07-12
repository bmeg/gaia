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
import scalaz.stream.text
import scalaz.stream.Process
import scalaz.stream.Process._
import scalaz.stream.Process1
import scalaz.concurrent.Task
import scala.collection.JavaConversions._

object MessageFacet extends LazyLogging {
  def ingestMessage(graph: TitanGraph) (line: String): Task[Vertex] = Task {
    val json = Ingest.parseJson(line)
    Ingest.ingestVertex(graph) (json)
  }

  val service = HttpService {
    case request @ POST -> Root / "ingest" =>
      val messages = request.bodyAsText.pipe(text.lines(1024 * 1024 * 64)).flatMap { line =>
        Process eval ingestMessage(Titan.connection) (line)
      }

      messages.runLog.run
      Ok(jString("done!"))
  }
}
