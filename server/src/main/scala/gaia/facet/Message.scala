package gaia.facet

import gaia.graph._
import gaia.ingest._
import gaia.collection.Collection._

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

case class MessageFacet(root: String) extends GaiaFacet with LazyLogging {
  def ingestMessage(ingestor: GaiaIngestor) (line: String): Task[Unit] = Task {
    ingestor.ingestMessage(line)
  }

  def service(graph: GaiaGraph): HttpService = {
    val ingestor = GraphIngestor(graph)
    HttpService {
      case request @ POST -> Root / "ingest" =>
        val messages = request.bodyAsText.pipe(text.lines(1024 * 1024 * 64)).flatMap { line =>
          Process eval ingestMessage(ingestor) (line)
        }

        messages.runLog.run
        Ok(jString("done!"))
    }
  }
}
