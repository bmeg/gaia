package gaia.ingest

import gaia.graph._
import gaia.io.JsonIO
import gaia.schema.Protograph._
import FieldAction.Action
import gremlin.scala._

import scala.collection.JavaConverters._

case class GraphTransform(graph: GaiaGraph) extends MessageTransform with GaiaIngestor {
  val keymap = collection.mutable.Map[String, Key[Any]]()

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

  def setProperty(vertex: Vertex) (field: Tuple2[String, Any]): Unit = {
    val key = camelize(field._1)
    field._2 match {
      case value: String =>
        vertex.setProperty(findKey[String](key), value)
      case value: Double =>
        vertex.setProperty(findKey[Double](key), value)
      case value: Boolean =>
        vertex.setProperty(findKey[Boolean](key), value)
      case value: Int =>
        vertex.setProperty(findKey[Long](key), value.toLong)
      case value: List[Any] =>
        vertex.setProperty(findKey[String](key), JsonIO.writeList(value))
      case _ =>
        println("unsupported key: " + key, field._2)
    }
  }

  def setProperties(vertex: Vertex) (prefix: String) (fields: List[Tuple2[String, Any]]): Unit = {
    for (field <- fields) {
      setProperty(vertex) ((prefix + "." + field._1, field._2))
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
      println(gids)
      println(gids.getClass)
      gids.asInstanceOf[List[Map[String, String]]].foreach { gidMap =>
        gidMap.values.foreach { gid =>
          graph.associateOut(vertex) (edges.edgeLabel) (edges.destinationLabel) (gid)
        }
      }
    }
    vertex
  }

  def renameProperty(vertex: Vertex) (rename: RenameProperty) (data: Map[String, Any]) (field: String): Vertex = {
    data.get(field).map { value =>
      setProperty(vertex) ((rename.rename, value))
    }
    vertex
  }

  def serializeField(vertex: Vertex) (map: SerializeField) (data: Map[String, Any]) (field: String): Vertex = {
    data.get(field).map { inner =>
      val json = JsonIO.write(inner)
      setProperty(vertex) ((map.serializedName, json))
    }
    vertex
  }

  def spliceMap(vertex: Vertex) (map: SpliceMap) (data: Map[String, Any]) (field: String): Vertex = {
    data.get(field).map { inner =>
      inner.asInstanceOf[Map[String, Any]].map { pair =>
        setProperty(vertex) ((map.prefix + "." + pair._1, pair._2))
      }
    }
    vertex
  }

  def innerVertex(graph: GaiaGraph) (vertex: Vertex) (inner: InnerVertex) (data: Map[String, Any]) (field: String): Vertex = {
    data.get(field).map { nested =>
      nested.asInstanceOf[List[Map[String, Any]]].map { nest =>
        val embedded = nest + (inner.outerId -> data.get("gid").get)
        val in = ingestVertex(inner.destinationLabel) (embedded)
        val innerGid = in.value[String]("gid")
        graph.associateOut(vertex) (inner.edgeLabel) (inner.destinationLabel) (innerGid)
      }
    }
    vertex
  }

  def joinList(vertex: Vertex) (list: JoinList) (data: Map[String, Any]) (field: String): Vertex = {
    data.get(field).map { inner =>
      val join = inner.asInstanceOf[List[Any]].map(_.toString).mkString(list.delimiter)
      setProperty(vertex) ((field, join))
    }
    vertex
  }

  def storeField(vertex: Vertex) (store: StoreField) (data: Map[String, Any]) (field: String): Vertex = {
    if (store.store) {
      data.get(field).map { inner =>
        setProperty(vertex) ((field, inner))
      }
    }
    vertex
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

