package gaia.transform

import gaia.graph._
import gaia.ingest._
import gaia.protograph._
import gaia.io.JsonIO
import gaia.schema.Protograph._
import FieldAction.Action
import gremlin.scala._

import scala.collection.JavaConverters._

case class PartialEdge(from: Option[String], label: Option[String], to: Option[String], properties: Map[String, Any]) {
  def isComplete() {
    !from.isEmpty && !label.isEmpty && !to.isEmpty
  }
}

case class GraphTransform(graph: GaiaGraph) extends MessageTransform with GaiaIngestor {
  val keymap = collection.mutable.Map[String, Key[Any]]()
  val partialEdges = collection.mutable.Map[String, PartialEdge]()

  def findKey[T](key: String): Key[T] = {
    val newkey = keymap.get(key).getOrElse {
      val newkey = Key[Any](key)
      keymap(key) = newkey
      newkey
    }

    newkey.asInstanceOf[Key[T]]
  }

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

  def setProperty(element: Element) (field: Tuple2[String, Any]): Unit = {
    val key = camelize(field._1)
    field._2 match {
      case value: String =>
        element.setProperty(findKey[String](key), value)
      case value: Double =>
        element.setProperty(findKey[Double](key), value)
      case value: Boolean =>
        element.setProperty(findKey[Boolean](key), value)
      case value: Int =>
        element.setProperty(findKey[Long](key), value.toLong)
      case value: List[Any] =>
        element.setProperty(findKey[String](key), JsonIO.writeList(value))
      case _ =>
        println("unsupported key: " + key, field._2)
    }
  }

  def setProperties(element: Element) (prefix: String) (fields: List[Tuple2[String, Any]]): Unit = {
    for (field <- fields) {
      setProperty(element) ((prefix + "." + field._1, field._2))
    }
  }

  def associateEdge(graph: GaiaGraph) (vertex: Vertex) (edge: SingleEdge) (data: Map[String, Any]) (field: String): Vertex = {
    data.get(field).map { gid =>
      graph.associateOut(vertex) (edge.edgeLabel) (edge.destinationLabel) (gid.asInstanceOf[String])
    }
    vertex
  }

  def associateEdges(graph: GaiaGraph) (vertex: Vertex) (edges: RepeatedEdges) (data: Map[String, Any]) (field: String): Vertex = {
    data.get(field).map { gids =>
      gids.asInstanceOf[List[String]].foreach { gid =>
        graph.associateOut(vertex) (edges.edgeLabel) (edges.destinationLabel) (gid)
      }
    }
    vertex
  }

  def unembedEdges(graph: GaiaGraph) (vertex: Vertex) (edges: EmbeddedEdges) (data: Map[String, Any]) (field: String): Vertex = {
    data.get(field).map { gids =>
      gids.asInstanceOf[List[Map[String, String]]].foreach { gidMap =>
        gidMap.get(edges.embeddedIn).map { gid =>
          graph.associateOut(vertex) (edges.edgeLabel) (edges.destinationLabel) (gid)
        }
      }
    }
    vertex
  }

  def linkThrough(graph: GaiaGraph) (vertex: Vertex) (link: LinkThrough) (data: Map[String, Any]) (field: String): PartialEdge = {
    data.get(field).map { through =>
      
    }
  }

  def edgeTerminal(graph: GaiaGraph) (vertex: Vertex) (edge: EdgeTerminal) (data: Map[String, Any]) (field: String): PartialEdge = {
    data.get(field).map { terminal =>

    }
  }

  def renameProperty(element: Element) (rename: RenameProperty) (data: Map[String, Any]) (field: String): Element = {
    data.get(field).map { value =>
      setProperty(element) ((rename.rename, value))
    }
    element
  }

  def serializeField(element: Element) (map: SerializeField) (data: Map[String, Any]) (field: String): Element = {
    data.get(field).map { inner =>
      val json = JsonIO.write(inner)
      setProperty(element) ((map.serializedName, json))
    }
    element
  }

  def spliceMap(element: Element) (map: SpliceMap) (data: Map[String, Any]) (field: String): Element = {
    data.get(field).map { inner =>
      inner.asInstanceOf[Map[String, Any]].map { pair =>
        setProperty(element) ((map.prefix + "." + pair._1, pair._2))
      }
    }
    element
  }

  def innerVertex(graph: GaiaGraph) (vertex: Vertex) (inner: InnerVertex) (data: Map[String, Any]) (field: String): Vertex = {
    def extract(nest: Map[String, Any]) {
      val embedded = nest + (inner.outerId -> data.get("gid").get)
      val in = ingestVertex(inner.destinationLabel) (embedded)
      val innerGid = in.value[String]("gid")
      graph.associateOut(vertex) (inner.edgeLabel) (inner.destinationLabel) (innerGid)
    }

    data.get(field).map { nested =>
      nested match {
        case inner: List[Map[String, Any]] => inner.map(extract)
        case inner: Map[String, Any] => extract(inner)
      }
    }
    vertex
  }

  def joinList(element: Element) (list: JoinList) (data: Map[String, Any]) (field: String): Element = {
    data.get(field).map { inner =>
      val join = inner.asInstanceOf[List[Any]].map(_.toString).mkString(list.delimiter)
      setProperty(element) ((field, join))
    }
    element
  }

  def storeField(element: Element) (store: StoreField) (data: Map[String, Any]) (field: String): Element = {
    if (store.store) {
      data.get(field).map { inner =>
        setProperty(element) ((field, inner))
      }
    }
    element
  }

  def ingestVertex(label: String) (data: Map[String, Any]): Vertex = {
    // find the transform description for vertexes with this label
    val protograph = graph.schema.protograph.transformFor(label)

    // Determine the GID from the message
    val gid = protograph.gid(data)
    val global = data + ("gid" -> gid)

    println("GID: " + gid)

    // Start decorating the vertex
    val vertex = findVertex(graph) (label) (gid)

    val protovertex = protograph.transform.actions.foldLeft(vertex) { (vertex, action) =>
      action.action match {
        case Action.SingleEdge(edge) => associateEdge(graph) (vertex) (edge) (global) (action.field)
        case Action.RepeatedEdges(edges) => associateEdges(graph) (vertex) (edges) (global) (action.field)
        case Action.EmbeddedEdges(edges) => unembedEdges(graph) (vertex) (edges) (global) (action.field)
        case Action.LinkThrough(link) => linkThrough(graph) (vertex) (link) (global) (action.field)
        case Action.EdgeTerminal(edge) => edgeTerminal(graph) (vertex) (edge) (global) (action.field)
        case Action.RenameProperty(field) => renameProperty(vertex) (field) (global) (action.field)
        case Action.SerializeField(map) => serializeField(vertex) (map) (global) (action.field)
        case Action.SpliceMap(map) => spliceMap(vertex) (map) (global) (action.field)
        case Action.InnerVertex(inner) => innerVertex(graph) (vertex) (inner) (global) (action.field)
        case Action.JoinList(list) => joinList(vertex) (list) (global) (action.field)
        case Action.StoreField(store) => storeField(vertex) (store) (global) (action.field)
      }
    }

    val remaining = protograph.transform.actions.map(_.field).foldLeft(global) ((data, field) =>
      data - field
    )

    val fullVertex = remaining.foldLeft(protovertex) { (vertex, pair) =>
      setProperty(vertex) (pair)
      vertex
    }

    graph.commit()
    fullVertex
  }

  def transform(message: Map[String,Any]) {
    val label = stringFor(message) ("#label")
    val vertex = ingestVertex(label) (message)
  }

  def ingestMessage(label: String) (message: String) {
    val map = JsonIO.readMap(message)
    transform(map + ("#label" -> label))
  }
}

