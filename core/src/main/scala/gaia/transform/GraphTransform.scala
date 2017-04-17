package gaia.transform

import gaia.graph._
import gaia.ingest._
import gaia.protograph._
import gaia.io.JsonIO
import gaia.schema.Protograph._
import FieldAction.Action
import gremlin.scala._

import scala.collection.JavaConverters._

case class PartialVertex(gid: String, properties: Map[String, Any])
case class PartialEdge(
  fromLabel: Option[String] = None,
  from: Option[String] = None,
  edgeLabel: Option[String] = None,
  to: Option[String] = None,
  toLabel: Option[String] = None,
  properties: Map[String, Any] = Map[String, Any]())

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
      // case value: String =>
      //   element.setProperty(findKey[String](key), value)
      // case value: Double =>
      //   element.setProperty(findKey[Double](key), value)
      // case value: Boolean =>
      //   element.setProperty(findKey[Boolean](key), value)
      // case value: Int =>
      //   element.setProperty(findKey[Long](key), value.toLong)
      // case value: List[Any] =>
      //   element.setProperty(findKey[String](key), JsonIO.writeList(value))
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

  def associateEdge(graph: GaiaGraph) (vertex: Vertex) (edge: SingleEdge) (data: Map[String, Any]) (field: String): Unit = {
    data.get(field).map { gid =>
      graph.associateOut(vertex, edge.edgeLabel, edge.destinationLabel, gid.asInstanceOf[String])
      // graph.associateOut(vertex) (edge.edgeLabel) (edge.destinationLabel) (gid.asInstanceOf[String])
    }
  }

  def associateEdges(graph: GaiaGraph) (vertex: Vertex) (edges: RepeatedEdges) (data: Map[String, Any]) (field: String): Unit = {    
    data.get(field).map { gids =>
      gids.asInstanceOf[List[String]].foreach { gid =>
        graph.associateOut(vertex, edges.edgeLabel, edges.destinationLabel, gid)
        // graph.associateOut(vertex) (edges.edgeLabel) (edges.destinationLabel) (gid)
      }
    }
  }

  def unembedEdges(graph: GaiaGraph) (vertex: Vertex) (edges: EmbeddedEdges) (data: Map[String, Any]) (field: String): Unit = {
    data.get(field).map { gids =>
      gids.asInstanceOf[List[Map[String, String]]].foreach { gidMap =>
        gidMap.get(edges.embeddedIn).map { gid =>
          graph.associateOut(vertex, edges.edgeLabel, edges.destinationLabel, gid)
          // graph.associateOut(vertex) (edges.edgeLabel) (edges.destinationLabel) (gid)
        }
      }
    }
  }

  def linkThrough(graph: GaiaGraph) (gid: String) (vertex: Vertex) (link: LinkThrough) (data: Map[String, Any]) (field: String): Unit = {
    data.get(field).map { through =>
      val throughAsString: String = through.asInstanceOf[String]
      val key = link.edgeLabel + throughAsString
      val existing = partialEdges.get(key)
      if (existing.isEmpty) {
        val partial = PartialEdge(from = Some(gid), edgeLabel = Some(link.edgeLabel))
        partialEdges += (throughAsString -> partial)
      } else {
        graph.associateOut(vertex, link.edgeLabel, link.destinationLabel, existing.get.to.get)
        // graph.associateOut (vertex) (existing.label) (link.destinationLabel) (existing.to)
      }
    }
  }

  def edgeSource(graph: GaiaGraph) (gid: String) (edge: EdgeSource) (data: Map[String, Any]) (field: String): Unit = {
    data.get(field).map { source =>
      val key = edge.edgeLabel + gid
      val existing = partialEdges.get(key)
      if (existing.isEmpty) {
        partialEdges += (key -> PartialEdge(edgeLabel = Some(edge.edgeLabel), from = Some(source.asInstanceOf[String]), fromLabel = Some(edge.destinationLabel), properties=data))
      } else {
        val from = findVertex(graph) (edge.destinationLabel) (gid)
        graph.associateOut(from, edge.edgeLabel, edge.destinationLabel, existing.get.to.get, existing.get.properties)
        // graph.associateOut(from) (edge.edgeLabel) (edge.destinationLabel) (existing.to) (existing.properties)
      }
    }
  }

  def edgeTerminal(graph: GaiaGraph) (gid: String) (edge: EdgeTerminal) (data: Map[String, Any]) (field: String): Unit = {
    data.get(field).map { terminal =>
      val key = edge.edgeLabel + gid
      val existing = partialEdges.get(key)
      val terminalAsString = terminal.asInstanceOf[String]
      if (existing.isEmpty) {
        partialEdges += (key -> PartialEdge(edgeLabel = Some(edge.edgeLabel), to = Some(terminalAsString), toLabel = Some(edge.destinationLabel), properties=data))
      } else {
        val from = findVertex(graph) (existing.get.fromLabel.get) (existing.get.from.get)
        graph.associateOut(from, edge.edgeLabel, edge.destinationLabel, terminalAsString, existing.get.properties)
        // graph.associateOut(from) (edge.edgeLabel) (edge.destinationLabel) (gid) (existing.properties)
      }
    }
  }

  def renameProperty(element: Element) (rename: RenameProperty) (data: Map[String, Any]) (field: String): Unit = {
    data.get(field).map { value =>
      setProperty(element) ((rename.rename, value))
    }
  }

  def serializeField(element: Element) (map: SerializeField) (data: Map[String, Any]) (field: String): Unit = {
    data.get(field).map { inner =>
      val json = JsonIO.write(inner)
      setProperty(element) ((map.serializedName, json))
    }
  }

  def spliceMap(element: Element) (map: SpliceMap) (data: Map[String, Any]) (field: String): Unit = {
    data.get(field).map { inner =>
      inner.asInstanceOf[Map[String, Any]].map { pair =>
        setProperty(element) ((map.prefix + "." + pair._1, pair._2))
      }
    }
  }

  def innerVertex(graph: GaiaGraph) (vertex: Vertex) (inner: InnerVertex) (data: Map[String, Any]) (field: String): Unit = {
    def extract(nest: Map[String, Any]) {
      val embedded = nest + (inner.outerId -> data.get("gid").get)
      val in = ingestVertex(inner.destinationLabel) (embedded)
      val innerGid = in.value[String]("gid")
      graph.associateOut(vertex, inner.edgeLabel, inner.destinationLabel, innerGid)
      // graph.associateOut(vertex) (inner.edgeLabel) (inner.destinationLabel) (innerGid)
    }

    data.get(field).map { nested =>
      nested match {
        case inner: List[Map[String, Any]] => inner.map(extract)
        case inner: Map[String, Any] => extract(inner)
      }
    }
  }

  def joinList(element: Element) (list: JoinList) (data: Map[String, Any]) (field: String): Unit = {
    data.get(field).map { inner =>
      val join = inner.asInstanceOf[List[Any]].map(_.toString).mkString(list.delimiter)
      setProperty(element) ((field, join))
    }
  }

  def storeField(element: Element) (store: StoreField) (data: Map[String, Any]) (field: String): Unit = {
    if (store.store) {
      data.get(field).map { inner =>
        setProperty(element) ((field, inner))
      }
    }
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

    val protovertex = protograph.transform.actions.foreach { (action) =>
      action.action match {
        case Action.SingleEdge(edge) => associateEdge(graph) (vertex) (edge) (global) (action.field)
        case Action.RepeatedEdges(edges) => associateEdges(graph) (vertex) (edges) (global) (action.field)
        case Action.EmbeddedEdges(edges) => unembedEdges(graph) (vertex) (edges) (global) (action.field)
        case Action.LinkThrough(link) => linkThrough(graph) (gid) (vertex) (link) (global) (action.field)
        case Action.EdgeSource(edge) => edgeSource(graph) (gid) (edge) (global) (action.field)
        case Action.EdgeTerminal(edge) => edgeTerminal(graph) (gid) (edge) (global) (action.field)
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

    remaining.foreach { (pair) =>
      setProperty(vertex) (pair)
    }

    graph.commit()
    vertex
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

