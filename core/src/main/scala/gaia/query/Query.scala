package gaia.query

import gaia.graph._
import gaia.io.JsonIO
import gaia.schema.Protograph._
import FieldAction.Action

import ophion.Ophion._
import gremlin.scala._

import org.json4s._
import org.json4s.jackson._
import org.json4s.jackson.JsonMethods._

import scala.collection.JavaConversions._

case class GaiaQuery(query: Query) {
  implicit val formats = DefaultFormats

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

  def executeJson(graph: GaiaGraph): String = {
    val result = GaiaQuery.resultJson(graph) (execute(graph))
    compact(render(result))
  }
}

object GaiaQuery {
  implicit val formats = DefaultFormats
  def parse(raw: String): GaiaQuery = GaiaQuery(Query.fromString(raw))

  def renderJson(result: List[Any]): String = {
    compact(render(Query.resultJson(result)))
  }

  def translateVertex(graph: GaiaGraph) (vertex: Vertex): VertexDirect = {
    val view: VertexDirect = GraphView.translateVertex(vertex).asInstanceOf[VertexDirect]
    // val label = view.properties.get("#label").getOrElse("default").asInstanceOf[String]
    val protograph = graph.schema.protograph.transformFor(view.`type`)
    val properties = protograph.transform.actions.foldLeft(view.properties) { (data, action) =>
      action.action match {
        case Action.SerializeField(map) => {
          data.get(map.serializedName).map { serial =>
            val unserial = JsonIO.read[Any](serial.asInstanceOf[String])
            (data - map.serializedName) + (action.field -> unserial)
          }.getOrElse(data)
        }

        case _ => data
      }
    }

    VertexDirect(view.`type`, properties)
  }

  def convertResult(graph: GaiaGraph) (item: Any): Any = {
    item match {
      case item: Vertex => translateVertex(graph) (item)
      case item: Edge => GraphView.translateEdge(item)

      case item: java.util.HashMap[String, Any] => {
        val map = item.toMap
        map.mapValues(convertResult(graph))
      }

      case item: java.util.LinkedHashMap[String, Any] => {
        val map = item.toMap
        map.mapValues(convertResult(graph))
      }

      case _ => {
        println("unsupported export type in query")
        println(item.getClass)
        item
      }
    }
  }

  def queryResult(graph: GaiaGraph) (result: List[Any]): List[Any] = {
    result.map(convertResult(graph))
  }

  def resultJson(graph: GaiaGraph) (result: List[Any]): JValue = {
    val translation = queryResult(graph) (result)
    val output = Map("result" -> translation)
    Extraction.decompose(output)
  }
}
