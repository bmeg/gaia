package gaia.facet

import org.http4s._
import org.http4s.server._
import org.http4s.dsl._

import scalaz.concurrent.Task
import java.io.File
import com.typesafe.scalalogging._

object StaticFacet extends LazyLogging {
  val service = HttpService {
    case req @ GET -> "static" /: path =>
      val localPath = new File(new File("./resources/public/static"), path.toString)
      StaticFile.fromFile(localPath, Some(req)).fold(NotFound())(Task.now)

    case req @ GET -> Root =>
      val localPath = new File(new File("./resources/public/static"), "main.html")
      StaticFile.fromFile(localPath, Some(req)).fold(NotFound())(Task.now)
  }
}
