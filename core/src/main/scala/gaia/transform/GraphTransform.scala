package gaia.transform

import gaia.graph._
import gaia.ingest._
import gaia.protograph._
import gaia.io.JsonIO
import gaia.schema.Protograph._
import FieldAction.Action
import gremlin.scala._

import scala.collection.JavaConverters._

case class GaiaVertex(
  label: String,
  gid: String,
  properties: Map[String, Any] = Map[String, Any]()
)

case class GaiaEdge(
  label: String,
  fromLabel: String,
  toLabel: String,
  from: String,
  to: String,
  properties: Map[String, Any] = Map[String, Any]()
)

case class PartialEdge(
  fromLabel:  Option[String] = None,
  from:       Option[String] = None,
  label:  Option[String] = None,
  toLabel:    Option[String] = None,
  to:         Option[String] = None,
  properties: Map[String, Any] = Map[String, Any]()
)

trait GraphEmitter {
  def emitVertex(vertex: GaiaVertex)
  def emitEdge(edge: GaiaEdge)
}

case class GraphTransform(graph: GaiaGraph) extends MessageTransform with GaiaIngestor {
  val partialEdges = collection.mutable.Map[String, List[PartialEdge]]()
  val printEmitter = new GraphEmitter {
    def emitVertex(vertex: GaiaVertex) {
      println(vertex)
    }

    def emitEdge(edge: GaiaEdge) {
      println(edge)
    }
  }

  def addPartialEdge(gid: String) (edge: PartialEdge): Unit = {
    val here = edge +: partialEdges.getOrElse(gid, List[PartialEdge]())
    partialEdges += (gid -> here)
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

  def ensureSeq(x: Any): Seq[Any] = x match {
    case x: Seq[_] => x
    case _ => List(x)
  }

  def unembed(field: Any, embeddedIn: String): Option[String] = {
    if (embeddedIn.isEmpty) {
      Some(field.asInstanceOf[String])
    } else {
      field.asInstanceOf[Map[String, String]].get(embeddedIn)
    }
  }

  def associateEdges(emit: GraphEmitter) (proto: EdgeDescription) (vertex: GaiaVertex) (field: Option[Any]): Map[String, Any] = {
    field.map { remote =>
      ensureSeq(remote).map { remote =>
        unembed(remote, proto.embeddedIn).map { in =>
          val edge = GaiaEdge(
            label = proto.edgeLabel,
            fromLabel = vertex.label,
            toLabel = proto.destinationLabel,
            from = vertex.gid,
            to = in
          )

          emit.emitEdge(edge)
          // graph.associateOut(vertex, proto.edgeLabel, proto.destinationLabel, in)
        }
      }
    }

    Map[String, Any]()
  }

  def linkThrough(emit: GraphEmitter) (proto: EdgeDescription) (vertex: GaiaVertex) (field: Option[Any]): Map[String, Any] = {
    field.map { through =>
      ensureSeq(through).map { through =>
        unembed(through, proto.embeddedIn).map { through =>
          val key = proto.edgeLabel + through
          val existing = partialEdges.getOrElse(key, List[PartialEdge]())
          if (existing.isEmpty) {
            val partial = PartialEdge(
              label = Some(proto.edgeLabel),
              fromLabel = Some(vertex.label),
              toLabel = Some(proto.destinationLabel),
              from = Some(vertex.gid)
            )

            // println("partialEdge", proto.edgeLabel, through, proto.destinationLabel)
            partialEdges += (key -> List(partial))
          } else {
            existing.foreach { exist =>
              val edge = GaiaEdge(
                label = proto.edgeLabel,
                fromLabel = vertex.label,
                toLabel = proto.destinationLabel,
                from = vertex.gid,
                to = exist.to.get,
                properties = exist.properties
              )

              emit.emitEdge(edge)

              // println("linkThrough", vertex.gid, proto.edgeLabel, proto.destinationLabel, exist.to.get, exist.properties)
              // graph.associateOut(vertex, proto.edgeLabel, proto.destinationLabel, exist.to.get)
            }
          }
        }
      }
    }

    Map[String, Any]()
  }

  def edgeSource(emit: GraphEmitter) (proto: EdgeDescription) (gid: String) (field: Option[Any]) (data: Map[String, Any]): Map[String, Any] = {
    field.map { source =>
      val key = proto.edgeLabel + gid // source.asInstanceOf[String]
      val existing = partialEdges.getOrElse(key, List[PartialEdge]())
      ensureSeq(source).map { source =>
        val sourceString = source.asInstanceOf[String]
        if (existing.isEmpty) {
          // println(proto.edgeLabel, sourceString, proto.destinationLabel)

          val partial = PartialEdge(
            label = Some(proto.edgeLabel),
            fromLabel = Some(proto.destinationLabel),
            from = Some(sourceString),
            properties = data
          )

          addPartialEdge(key) (partial)
        } else {
          // val from = findVertex(graph) (proto.destinationLabel) (sourceString)
          existing.foreach { exist =>
            // println("edgeSource", sourceString, proto.edgeLabel, exist.toLabel.get, exist.to.get, exist.properties)

            val edge = GaiaEdge(
              label = proto.edgeLabel,
              fromLabel = proto.destinationLabel,
              toLabel = exist.toLabel.get,
              from = sourceString,
              to = exist.to.get,
              properties = exist.properties ++ data
            )

            emit.emitEdge(edge)
            // graph.associateOut(from, proto.edgeLabel, proto.destinationLabel, existing.get.to.get, existing.get.properties)
          }
        }
      }
    }

    Map[String, Any]()
  }

  def edgeTerminal(emit: GraphEmitter) (proto: EdgeDescription) (gid: String) (field: Option[Any]) (data: Map[String, Any]): Map[String, Any] = {
    field.map { terminal =>
      val key = proto.edgeLabel + gid
      val existing = partialEdges.getOrElse(key, List[PartialEdge]())
      ensureSeq(terminal).map { terminal =>
        if (existing.isEmpty) {
          // println("partialEdge", proto.edgeLabel, key, proto.destinationLabel)

          val partial = PartialEdge(
            label = Some(proto.edgeLabel),
            to = Some(terminal.asInstanceOf[String]),
            toLabel = Some(proto.destinationLabel),
            properties=data
          )

          addPartialEdge(key) (partial)
        } else {
          existing.map { exist =>
            // val from = findVertex(graph) (exist.fromLabel.get) (exist.from.get)

            // println("edgeTerminal", exist.from.get, proto.edgeLabel, proto.destinationLabel, terminal)

            val edge = GaiaEdge(
              label = proto.edgeLabel,
              fromLabel = exist.fromLabel.get,
              toLabel = proto.destinationLabel,
              from = exist.from.get,
              to = terminal.asInstanceOf[String],
              properties = exist.properties ++ data
            )

            emit.emitEdge(edge)
            // graph.associateOut(from, proto.edgeLabel, proto.destinationLabel, terminalAsString, existing.get.properties)
          }
        }
      }
    }

    Map[String, Any]()
  }

  def embeddedTerminals(emit: GraphEmitter) (proto: EdgeDescription) (gid: String) (field: Option[Any]) (data: Map[String, Any]): Map[String, Any] = {
    field.map { terminal =>
      val key = proto.edgeLabel + gid
      val existing = partialEdges.getOrElse(key, List[PartialEdge]())
      ensureSeq(terminal).map { terminal =>
        val terminalMap = terminal.asInstanceOf[Map[String, Any]]
        if (existing.isEmpty) {
          terminalMap.get(proto.embeddedIn).map { id =>
            // println("partialEdge", proto.edgeLabel, key, proto.destinationLabel)
            val partial = PartialEdge(
              label = Some(proto.edgeLabel),
              to = Some(id.asInstanceOf[String]),
              toLabel = Some(proto.destinationLabel),
              properties = (data ++ terminalMap))
            addPartialEdge(key) (partial)
          }
        } else {
          existing.foreach { exist =>
            val from = findVertex(graph) (exist.fromLabel.get) (exist.from.get)
            terminalMap.get(proto.embeddedIn).map { id =>
              // println("edgeTerminal", exist.from.get, proto.edgeLabel, proto.destinationLabel, id.asInstanceOf[String], exist.properties)

              val edge = GaiaEdge(
                label = proto.edgeLabel,
                fromLabel = exist.fromLabel.get,
                toLabel = proto.destinationLabel,
                from = exist.from.get,
                to = id.asInstanceOf[String],
                properties = exist.properties ++ data
              )

              emit.emitEdge(edge)
            }
            // graph.associateOut(from, proto.edgeLabel, proto.destinationLabel, terminalAsString, existing.get.properties)
          }
        }
      }
    }

    Map[String, Any]()
  }

  def innerVertex(protograph: Protograph) (emit: GraphEmitter) (proto: InnerVertex) (vertex: GaiaVertex) (field: Option[Any]): Map[String, Any] = {
    def extract(nest: Map[String, Any]) {
      val embedded = nest + (proto.outerId -> vertex.gid)
      val inner = processVertex(protograph) (emit) (proto.destinationLabel) (embedded)
      val edge = GaiaEdge(
        label = proto.edgeLabel,
        fromLabel = vertex.label,
        toLabel = proto.destinationLabel,
        from = vertex.gid,
        to = inner.gid
      )

      emit.emitVertex(vertex)
      emit.emitEdge(edge)
      // val in = ingestVertex(proto.destinationLabel) (embedded)
      // val innerGid = in.value[String]("gid")
      // graph.associateOut(vertex, proto.edgeLabel, proto.destinationLabel, innerGid)
    }

    field.map { nested =>
      nested match {
        case inner: List[Map[String, Any]] => inner.map(extract)
        case inner: Map[String, Any] => extract(inner)
      }
    }

    Map[String, Any]()
  }

  def renameProperty(rename: RenameProperty) (field: Option[Any]): Map[String, Any] = {
    field.map { value =>
      Map[String, Any](rename.rename -> value)
      // setProperty(element) ((rename.rename, value))
    }.getOrElse(Map[String, Any]())
  }

  def serializeField(map: SerializeField) (field: Option[Any]): Map[String, Any] = {
    field.map { inner =>
      val json = JsonIO.write(inner)
      Map[String, Any](map.serializedName -> json)
      // setProperty(element) ((map.serializedName, json))
    }.getOrElse(Map[String, Any]())
  }

  def spliceMap(map: SpliceMap) (field: Option[Any]): Map[String, Any] = {
    field.map { inner =>
      inner.asInstanceOf[Map[String, Any]].map { pair =>
        val key = map.prefix + "." + pair._1
        (key -> pair._2)
        // setProperty(element) ((map.prefix + "." + pair._1, pair._2))
      }
    }.getOrElse(Map[String, Any]())
  }

  def joinList(list: JoinList) (key: String) (field: Option[Any]): Map[String, Any] = {
    field.map { inner =>
      val join = inner.asInstanceOf[List[Any]].map(_.toString).mkString(list.delimiter)
      Map[String, Any](key -> join)
      // setProperty(element) ((key, join))
    }.getOrElse(Map[String, Any]())
  }

  def storeField(store: StoreField) (key: String) (field: Option[Any]): Map[String, Any] = {
    if (store.store) {
      field.map { inner =>
        Map[String, Any](key -> inner)
        // setProperty(element) ((key, inner))
      }.getOrElse(Map[String, Any]())
    } else {
      Map[String, Any]()
    }
  }

  def processEdge(protograph: Protograph) (emit: GraphEmitter) (label: String) (data: Map[String, Any]): Unit = {
    val transform = protograph.transformFor(label)
    val gid = transform.gid(data)
    val properties = transform.transform.actions.map { action =>
      val key = action.field
      val field = data.get(key)
      action.action match {
        case Action.RenameProperty(rename) =>
          renameProperty(rename) (field)
        case Action.SerializeField(map) =>
          serializeField(map) (field)
        case Action.SpliceMap(map) =>
          spliceMap(map) (field)
        case Action.JoinList(join) =>
          joinList(join) (key) (field)
        case Action.StoreField(store) =>
          storeField(store) (key) (field)
        case _ =>
          Map[String, Any]()
      }
    }.reduce(_ ++ _) + ("gid" -> gid)

    transform.transform.actions.foreach { action =>
      val key = action.field
      val field = data.get(key)
      action.action match {
        case Action.EdgeSource(edge) =>
          edgeSource(emit) (edge) (gid) (field) (properties)
        case Action.EdgeTerminal(edge) =>
          edgeTerminal(emit) (edge) (gid) (field) (properties)
        case Action.EmbeddedTerminals(edge) =>
          embeddedTerminals(emit) (edge) (gid) (field) (properties)
        case _ =>
          Map[String, Any]()
      }
    }
  }

  def processVertex(protograph: Protograph) (emit: GraphEmitter) (label: String) (data: Map[String, Any]): GaiaVertex = {
    val transform = protograph.transformFor(label)
    val gid = transform.gid(data)
    val vertex = GaiaVertex(
      label = label,
      gid = gid
    )

    val properties = transform.transform.actions.map { action =>
      // println(action.action)
      val key = action.field
      val field = data.get(key)
      action.action match {
        case Action.RemoteEdges(remote) =>
          associateEdges(emit) (remote) (vertex) (field)
        case Action.LinkThrough(link) =>
          linkThrough(emit) (link) (vertex) (field)
        case Action.InnerVertex(inner) =>
          innerVertex(protograph) (emit) (inner) (vertex) (field)
        case Action.RenameProperty(rename) =>
          renameProperty(rename) (field)
        case Action.SerializeField(map) =>
          serializeField(map) (field)
        case Action.SpliceMap(map) =>
          spliceMap(map) (field)
        case Action.JoinList(join) =>
          joinList(join) (key) (field)
        case Action.StoreField(store) =>
          storeField(store) (key) (field)
        case _ =>
          Map[String, Any]()
      }
    }.reduce(_ ++ _) + ("gid" -> gid)

    val remaining = transform.transform.actions.map(_.field).foldLeft(data) ((data, field) =>
      data - field
    )

    emit.emitVertex(vertex.copy(properties = properties ++ remaining))
    vertex
  }

  def processMessage(protograph: Protograph) (emit: GraphEmitter) (label: String) (data: Map[String, Any]): Unit = {
    protograph.transformFor(label).transform.role match {
      case "Vertex" => processVertex(protograph) (emit) (label) (data)
      case "Edge" => processEdge(protograph) (emit) (label) (data)
    }
  }

  def ingestVertex(label: String) (data: Map[String, Any]): Unit = {
    processMessage(graph.schema.protograph) (printEmitter) (label) (data)

    // // find the transform description for vertexes with this label
    // val protograph = graph.schema.protograph.transformFor(label)

    // // Determine the GID from the message
    // val gid = protograph.gid(data)
    // val global = data + ("gid" -> gid)

    // println("gid", gid)

    // val vertex = findVertex(graph) (label) (gid)

    // protograph.transform.actions.foreach { action =>
    //   println(action.action)
    //   val field = global.get(action.field)
    //   action.action match {
    //     case Action.RemoteEdges(edge) => associateEdges(graph) (vertex) (edge) (field)
    //     case Action.LinkThrough(edge) => linkThrough(graph) (gid) (vertex) (edge) (field)
    //     case Action.EdgeSource(edge) => edgeSource(graph) (gid) (edge) (field) (global)
    //     case Action.EdgeTerminal(edge) => edgeTerminal(graph) (gid) (edge) (field) (global)
    //     case Action.EmbeddedTerminals(edge) => embeddedTerminals(graph) (gid) (edge) (field) (global)
    //     case Action.RenameProperty(rename) => renameProperty(vertex) (rename) (field)
    //     case Action.SerializeField(map) => serializeField(vertex) (map) (field)
    //     case Action.SpliceMap(map) => spliceMap(vertex) (map) (field)
    //     case Action.InnerVertex(inner) => innerVertex(graph) (vertex) (inner) (field)
    //     case Action.JoinList(list) => joinList(vertex) (action.field) (list) (field)
    //     case Action.StoreField(store) => storeField(vertex) (action.field) (store) (field)
    //   }
    // }

    // val remaining = protograph.transform.actions.map(_.field).foldLeft(global) ((data, field) =>
    //   data - field
    // )

    // remaining.foreach { (pair) =>
    //   setProperty(vertex) (pair)
    // }

    // graph.commit()
    // vertex
  }

  def transform(message: Map[String,Any]) {
    val label = stringFor(message) ("#label")
    ingestVertex(label) (message)
  }

  def ingestMessage(label: String) (message: String) {
    val map = JsonIO.readMap(message)
    transform(map + ("#label" -> label))
  }
}

