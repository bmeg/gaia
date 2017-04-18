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
  fromLabel:  Option[String] = None,
  from:       Option[String] = None,
  edgeLabel:  Option[String] = None,
  to:         Option[String] = None,
  toLabel:    Option[String] = None,
  properties: Map[String, Any] = Map[String, Any]())

case class GraphTransform(graph: GaiaGraph) extends MessageTransform with GaiaIngestor {
  val keymap = collection.mutable.Map[String, Key[Any]]()
  var partialEdges = Map[String, List[PartialEdge]]()

  def addPartialEdge(gid: String) (edge: PartialEdge): Unit = {
    val here = edge +: partialEdges.getOrElse(gid, List[PartialEdge]())
    partialEdges = partialEdges + (gid -> here)
  }

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

  def associateEdge(graph: GaiaGraph) (vertex: Vertex) (edge: SingleEdge) (field: Option[Any]): Unit = {
    field.map { gid =>
      graph.associateOut(vertex, edge.edgeLabel, edge.destinationLabel, gid.asInstanceOf[String])
    }
  }

  def associateEdges(graph: GaiaGraph) (vertex: Vertex) (edges: RepeatedEdges) (field: Option[Any]): Unit = {    
    field.map { gids =>
      gids.asInstanceOf[List[String]].foreach { gid =>
        graph.associateOut(vertex, edges.edgeLabel, edges.destinationLabel, gid)
      }
    }
  }

  def unembedEdges(graph: GaiaGraph) (vertex: Vertex) (edges: EmbeddedEdges) (field: Option[Any]): Unit = {
    field.map { gids =>
      gids.asInstanceOf[List[Map[String, String]]].foreach { gidMap =>
        gidMap.get(edges.embeddedIn).map { gid =>
          graph.associateOut(vertex, edges.edgeLabel, edges.destinationLabel, gid)
        }
      }
    }
  }

  def linkThrough(graph: GaiaGraph) (gid: String) (vertex: Vertex) (link: LinkThrough) (field: Option[Any]): Unit = {
    field.map { through =>
      ensureSeq(through).foreach { through =>
        val key = link.edgeLabel + through.asInstanceOf[String]
        val existing = partialEdges.getOrElse(key, List[PartialEdge]())
        if (existing.isEmpty) {
          println(link.edgeLabel, through.asInstanceOf[String], link.destinationLabel)
          val partial = PartialEdge(from = Some(gid), edgeLabel = Some(link.edgeLabel))
          partialEdges += (through.asInstanceOf[String] -> List(partial))
        } else {
          existing.foreach { exist =>
            println("edgeSource", gid, link.edgeLabel, link.destinationLabel, exist.to.get, exist.properties)
            // graph.associateOut(vertex, link.edgeLabel, link.destinationLabel, exist.to.get)
          }
        }
      }
    }
  }

  def ensureSeq(x: Any): Seq[Any] = x match {
    case x: Seq[_] => x
    case _ => List(x)
  }

  def edgeSource(graph: GaiaGraph) (gid: String) (edge: EdgeSource) (field: Option[Any]) (data: Map[String, Any]): Unit = {
    println("in edge source")
    field.map { source =>
      ensureSeq(source).foreach { source =>
        val before = edge.edgeLabel + gid
        // val key = edge.edgeLabel + source.asInstanceOf[String]
        val existing = partialEdges.getOrElse(before, List[PartialEdge]())
        println(edge.edgeLabel, before, edge.destinationLabel)

        if (existing.isEmpty) {
          val key = edge.edgeLabel + source.asInstanceOf[String]
          addPartialEdge(key) (PartialEdge(edgeLabel = Some(edge.edgeLabel), from = Some(gid), fromLabel = Some(edge.destinationLabel), properties=data))
        } else {
          val from = findVertex(graph) (edge.destinationLabel) (gid)
          existing.foreach { exist =>
            println("edgeSource", gid, edge.edgeLabel, edge.destinationLabel, exist.to.get, exist.properties)
            // graph.associateOut(from, edge.edgeLabel, edge.destinationLabel, existing.get.to.get, existing.get.properties)
          }
        }
      }
    }
  }

  def edgeTerminal(graph: GaiaGraph) (gid: String) (edge: EdgeTerminal) (field: Option[Any]) (data: Map[String, Any]): Unit = {
    field.map { terminal =>
      val terminals = ensureSeq(terminal)
      val key = edge.edgeLabel + gid
      val existing = partialEdges.getOrElse(key, List[PartialEdge]())
      if (existing.isEmpty) {
        println(edge.edgeLabel, key, edge.destinationLabel)
        terminals.foreach { terminal =>
          addPartialEdge(key) (PartialEdge(edgeLabel = Some(edge.edgeLabel), to = Some(terminal.asInstanceOf[String]), toLabel = Some(edge.destinationLabel), properties=data))
        }
      } else {
        existing.foreach { exist =>
          val from = findVertex(graph) (exist.fromLabel.get) (exist.from.get)
          terminals.foreach { terminal =>
            println("edgeTerminal", exist.from.get, edge.edgeLabel, edge.destinationLabel, terminal)
            // graph.associateOut(from, edge.edgeLabel, edge.destinationLabel, terminalAsString, existing.get.properties)
          }
        }
      }
    }
  }

  def renameProperty(element: Element) (rename: RenameProperty) (field: Option[Any]): Unit = {
    field.map { value =>
      setProperty(element) ((rename.rename, value))
    }
  }

  def serializeField(element: Element) (map: SerializeField) (field: Option[Any]): Unit = {
    field.map { inner =>
      val json = JsonIO.write(inner)
      setProperty(element) ((map.serializedName, json))
    }
  }

  def spliceMap(element: Element) (map: SpliceMap) (field: Option[Any]): Unit = {
    field.map { inner =>
      inner.asInstanceOf[Map[String, Any]].map { pair =>
        setProperty(element) ((map.prefix + "." + pair._1, pair._2))
      }
    }
  }

  def innerVertex(graph: GaiaGraph) (vertex: Vertex) (inner: InnerVertex) (field: Option[Any]): Unit = {
    def extract(nest: Map[String, Any]) {
      val embedded = nest + (inner.outerId -> vertex.property("gid"))
      val in = ingestVertex(inner.destinationLabel) (embedded)
      val innerGid = in.value[String]("gid")
      graph.associateOut(vertex, inner.edgeLabel, inner.destinationLabel, innerGid)
      // graph.associateOut(vertex) (inner.edgeLabel) (inner.destinationLabel) (innerGid)
    }

    field.map { nested =>
      nested match {
        case inner: List[Map[String, Any]] => inner.map(extract)
        case inner: Map[String, Any] => extract(inner)
      }
    }
  }

  def joinList(element: Element) (key: String) (list: JoinList) (field: Option[Any]): Unit = {
    field.map { inner =>
      val join = inner.asInstanceOf[List[Any]].map(_.toString).mkString(list.delimiter)
      setProperty(element) ((key, join))
    }
  }

  def storeField(element: Element) (key: String) (store: StoreField) (field: Option[Any]): Unit = {
    if (store.store) {
      field.map { inner =>
        setProperty(element) ((key, inner))
      }
    }
  }

  def ingestVertex(label: String) (data: Map[String, Any]): Vertex = {
    // find the transform description for vertexes with this label
    val protograph = graph.schema.protograph.transformFor(label)

    // Determine the GID from the message
    val gid = protograph.gid(data)
    val global = data + ("gid" -> gid)

    println("gid", gid)

    val vertex = findVertex(graph) (label) (gid)

    println(label, "actions", protograph.transform.actions.size)
    protograph.transform.actions.foreach { action =>
      println(action.action)
      val field = global.get(action.field)
      action.action match {
        case Action.SingleEdge(edge) => associateEdge(graph) (vertex) (edge) (field)
        case Action.RepeatedEdges(edges) => associateEdges(graph) (vertex) (edges) (field)
        case Action.EmbeddedEdges(edges) => unembedEdges(graph) (vertex) (edges) (field)
        case Action.LinkThrough(link) => linkThrough(graph) (gid) (vertex) (link) (field)
        case Action.EdgeSource(edge) => edgeSource(graph) (gid) (edge) (field) (global)
        case Action.EdgeTerminal(edge) => edgeTerminal(graph) (gid) (edge) (field) (global)
        case Action.RenameProperty(rename) => renameProperty(vertex) (rename) (field)
        case Action.SerializeField(map) => serializeField(vertex) (map) (field)
        case Action.SpliceMap(map) => spliceMap(vertex) (map) (field)
        case Action.InnerVertex(inner) => innerVertex(graph) (vertex) (inner) (field)
        case Action.JoinList(list) => joinList(vertex) (action.field) (list) (field)
        case Action.StoreField(store) => storeField(vertex) (action.field) (store) (field)
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

