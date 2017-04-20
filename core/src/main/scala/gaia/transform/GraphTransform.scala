package gaia.transform

import gaia.graph._
import gaia.ingest._
import gaia.protograph._
import gaia.io.JsonIO
import gaia.schema.Protograph._
import FieldAction.Action
import gremlin.scala._

import scala.collection.JavaConverters._

case class GraphTransform(graph: GaiaGraph) extends MessageTransform with GaiaIngestor {
  def stringFor(obj: Map[String, Any]) (key: String): String = {
    (obj.get(key).get).asInstanceOf[String]
  }

  def camelize(s: String): String = {
    val break = s.split("_")
    val upper = break.head +: break.tail.map(_.capitalize)
    upper.mkString("")
  }

  def uncapitalize(s: String): String = {
    if (s.size > 0) {
      val c = s.toCharArray
      c(0) = Character.toLowerCase(c(0))
      new String(c)
    } else {
      ""
    }
  }

  def findVertex(graph: GaiaGraph) (label: String) (gid: String): Vertex = {
    val vertex = graph.namedVertex(label) (gid)
    graph.associateType(vertex) (label)
    vertex
  }

  def setProperty(element: Element) (field: Tuple2[String, Any]): Element = {
    val key = camelize(field._1)
    field._2 match {
      case value: String =>
        element.property(key, value)
      case value: Double =>
        element.property(key, value)
      case value: Boolean =>
        element.property(key, value)
      case value: Int =>
        element.property(key, value.toLong)
      case value: List[Any] =>
        element.property(key, JsonIO.writeList(value))
      case _ =>
        println("unsupported key: " + key, field._2)
    }

    element
  }

  def setProperties(element: Element) (prefix: String) (fields: List[Tuple2[String, Any]]): Element = {
    for (field <- fields) {
      setProperty(element) ((prefix + "." + field._1, field._2))
    }

    element
  }

  val graphEmitter = new ProtographEmitter {
    def emitVertex(proto: ProtoVertex) {
      val vertex = findVertex(graph) (proto.label) (proto.gid)
      proto.properties.foreach { property =>
        setProperty(vertex) (property)
      }
      graph.commit()
    }

    def emitEdge(proto: ProtoEdge) {
      val vertex = findVertex(graph) (proto.fromLabel) (proto.from)
      graph.associateOut(vertex, proto.label, proto.toLabel, proto.to, proto.properties)
    }
  }

  val printGraphEmitter = new ProtographEmitter {
    def emitVertex(proto: ProtoVertex) {
      graph.schema.protograph.printEmitter.emitVertex(proto)
      graphEmitter.emitVertex(proto)
    }

    def emitEdge(proto: ProtoEdge) {
      graph.schema.protograph.printEmitter.emitEdge(proto)
      graphEmitter.emitEdge(proto)
    }
  }

  def ingestVertex(label: String) (data: Map[String, Any]): Vertex = {
    val emit = graph.schema.protograph.printEmitter
    graph.schema.protograph.processMessage(emit) (label) (data)
    findVertex(graph) (label) ("type:type")
  }

  def transform(message: Map[String,Any]) {
    val label = stringFor(message) ("#label")
    ingestVertex(label) (message)
  }

  def ingestMessage(label: String) (message: String) {
    val map = JsonIO.readMap(message)
    val emit = printGraphEmitter
    // val emit = graph.schema.protograph.printEmitter
    // val emit = graphEmitter
    graph.schema.protograph.processMessage(emit) (label) (map)
    // transform(map + ("#label" -> label))
  }
}

