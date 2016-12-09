package gaia.query

import gaia.graph._
import ophion.Ophion._

import org.json4s._
import org.json4s.jackson._
import org.json4s.jackson.JsonMethods._

import scala.collection.JavaConversions._

case class GaiaQuery(query: Query) {
  def extractLabel: Option[String] = {
    query.query.head match {
      case LabelOperation(label) => Some(label)
      case _ => None
    }
  }

  def execute(graph: GaiaGraph): List[Any] = {
    extractLabel match {
      case Some(label) => query.interpret(graph.typeQuery(label).traversal).toList.toList
      case None => query.run(graph.graph)
    }
  }
}

object GaiaQuery {
  def parse(raw: String): GaiaQuery = GaiaQuery(Query.fromString(raw))

  def renderJson(result: List[Any]): String = {
    compact(render(Query.resultJson(result)))
  }
}
