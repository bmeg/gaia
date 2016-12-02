package gaea.query

import gaea.graph._
import ophion.Ophion._

import org.json4s._
import org.json4s.jackson._
import org.json4s.jackson.JsonMethods._

import scala.collection.JavaConversions._

case class GaeaQuery(query: Query) {
  def extractLabel: Option[String] = {
    query.query.head match {
      case LabelOperation(label) => Some(label)
      case _ => None
    }
  }

  def execute(graph: GaeaGraph): List[Any] = {
    extractLabel match {
      case Some(label) => query.interpret(graph.typeQuery(label).traversal).toList.toList
      case None => query.run(graph.graph)
    }
  }
}

object GaeaQuery {
  def parse(raw: String): GaeaQuery = GaeaQuery(Query.fromString(raw))

  def renderJson(result: List[Any]): String = {
    compact(render(Query.resultJson(result)))
  }
}
