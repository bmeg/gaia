package gaia.protograph

import gaia.io.JsonIO
import gaia.schema.{Graph, Vertex, Edge}
import gaia.schema.Protograph._
import FieldAction.Action
import gaia.file.mustache.Mustache

import java.io.FileInputStream
import scala.collection.mutable
import collection.JavaConverters._

import org.yaml.snakeyaml.Yaml
import com.fasterxml.jackson.core.{JsonGenerator}
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper, JsonSerializer, SerializerProvider}
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule

import com.trueaccord.scalapb.json.JsonFormat

case class ProtoVertex(
  label: String,
  gid: String,
  properties: Map[String, Any] = Map[String, Any]()
)

case class ProtoEdge(
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

class GidTemplate(template: String) extends Mustache(template) {
  def join(l: List[Any]): String = {
    println("joining " + l.toString)
    l.map(_.toString).mkString(",")
  }
}

case class ProtographTransform(transform: TransformMessage, template: GidTemplate) {
  def gid(data: Map[String, Any]): String = {
    data.get("gid").getOrElse {
      template.render(data)
    }.asInstanceOf[String]
  }
}

object ProtographTransform {
  def toProtograph(transform: TransformMessage): ProtographTransform = {
    ProtographTransform(transform, new GidTemplate(transform.gid))
  }
}

trait ProtographEmitter {
  def emitVertex(vertex: ProtoVertex)
  def emitEdge(edge: ProtoEdge)
}

case class Protograph(transforms: Seq[TransformMessage]) {
  val transformMap = transforms.map(step => (step.label, ProtographTransform.toProtograph(step))).toMap
  val default = TransformMessage(label = "default", gid = "default:{{gid}}")
  val defaultTransform = ProtographTransform.toProtograph(default)

  val partialEdges = collection.mutable.Map[String, List[PartialEdge]]()

  val printEmitter = new ProtographEmitter {
    def emitVertex(vertex: ProtoVertex) {
      println(vertex)
    }

    def emitEdge(edge: ProtoEdge) {
      println(edge)
    }
  }

  def addPartialEdge(gid: String) (edge: PartialEdge): Unit = {
    val here = edge +: partialEdges.getOrElse(gid, List[PartialEdge]())
    partialEdges += (gid -> here)
  }

  def transformFor(label: String): ProtographTransform = {
    transformMap.getOrElse(label, defaultTransform)
  }

  def graphStructure: Graph = {
    val emptyGraph = (List[Vertex](), List[Edge]())
    val (vertexes, edges) = transforms.foldLeft(emptyGraph) { (nodes, transform) =>
      nodes match {
        case (vertexes, previousEdges) =>
          val gid = transform.label
          val vertex = Vertex(gid=gid, label=transform.label)
          val edges = transform.actions.foldLeft(List[Edge]()) { (edges, action) =>
            action.action match {
              case Action.RemoteEdges(edge) =>
                Edge(label=edge.edgeLabel, in=gid, out=edge.destinationLabel) :: edges
              // case Action.SingleEdge(edge) =>
              //   Edge(label=edge.edgeLabel, in=gid, out=edge.destinationLabel) :: edges
              // case Action.RepeatedEdges(edge) =>
              //   Edge(label=edge.edgeLabel, in=gid, out=edge.destinationLabel) :: edges
              // case Action.EmbeddedEdges(edge) =>
              //   Edge(label=edge.edgeLabel, in=gid, out=edge.destinationLabel) :: edges
              case Action.InnerVertex(edge) =>
                Edge(label=edge.edgeLabel, in=gid, out=edge.destinationLabel) :: edges
              case _ => edges
            }
          }
          (vertex :: vertexes, edges ++ previousEdges)
      }
    }

    Graph.assemble(vertexes, edges)
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

  def associateEdges(emit: ProtographEmitter) (proto: EdgeDescription) (vertex: ProtoVertex) (field: Option[Any]): Map[String, Any] = {
    field.map { remote =>
      ensureSeq(remote).map { remote =>
        unembed(remote, proto.embeddedIn).map { in =>
          val edge = ProtoEdge(
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

  def linkThrough(emit: ProtographEmitter) (proto: EdgeDescription) (vertex: ProtoVertex) (field: Option[Any]): Map[String, Any] = {
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
              val edge = ProtoEdge(
                label = proto.edgeLabel,
                fromLabel = vertex.label,
                toLabel = proto.destinationLabel,
                from = vertex.gid,
                to = exist.to.get,
                properties = exist.properties
              )

              emit.emitEdge(edge)

              // println("linkThrough", vertex.gid, proto.edgeLabel, proto.destinationLabel, exist.to.get, exist.properties)
            }
          }
        }
      }
    }

    Map[String, Any]()
  }

  def edgeSource(emit: ProtographEmitter) (proto: EdgeDescription) (gid: String) (field: Option[Any]) (data: Map[String, Any]): Map[String, Any] = {
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
          existing.foreach { exist =>
            // println("edgeSource", sourceString, proto.edgeLabel, exist.toLabel.get, exist.to.get, exist.properties)

            val edge = ProtoEdge(
              label = proto.edgeLabel,
              fromLabel = proto.destinationLabel,
              toLabel = exist.toLabel.get,
              from = sourceString,
              to = exist.to.get,
              properties = exist.properties ++ data
            )

            emit.emitEdge(edge)
          }
        }
      }
    }

    Map[String, Any]()
  }

  def edgeTerminal(emit: ProtographEmitter) (proto: EdgeDescription) (gid: String) (field: Option[Any]) (data: Map[String, Any]): Map[String, Any] = {
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
            // println("edgeTerminal", exist.from.get, proto.edgeLabel, proto.destinationLabel, terminal)

            val edge = ProtoEdge(
              label = proto.edgeLabel,
              fromLabel = exist.fromLabel.get,
              toLabel = proto.destinationLabel,
              from = exist.from.get,
              to = terminal.asInstanceOf[String],
              properties = exist.properties ++ data
            )

            emit.emitEdge(edge)
          }
        }
      }
    }

    Map[String, Any]()
  }

  def embeddedTerminals(emit: ProtographEmitter) (proto: EdgeDescription) (gid: String) (field: Option[Any]) (data: Map[String, Any]): Map[String, Any] = {
    field.map { terminal =>
      val key = proto.edgeLabel + gid
      val existing = partialEdges.getOrElse(key, List[PartialEdge]())
      ensureSeq(terminal).map { terminal =>
        val terminalMap = terminal.asInstanceOf[Map[String, Any]]
        val lifted = proto.liftFields.foldLeft(Map[String, Any]()) { (outcome, lift) =>
          val inner = data.get(lift)
          if (!inner.isEmpty) {
            outcome ++ inner.get.asInstanceOf[List[Map[String, Any]]].reduce(_ ++ _)
          } else {
            outcome
          }
        }

        if (existing.isEmpty) {
          terminalMap.get(proto.embeddedIn).map { id =>
            // println("partialEdge", proto.edgeLabel, key, proto.destinationLabel)
            val partial = PartialEdge(
              label = Some(proto.edgeLabel),
              to = Some(id.asInstanceOf[String]),
              toLabel = Some(proto.destinationLabel),
              properties = (data ++ terminalMap ++ lifted) -- proto.liftFields
            )

            addPartialEdge(key) (partial)
          }
        } else {
          existing.foreach { exist =>
            terminalMap.get(proto.embeddedIn).map { id =>
              // println("edgeTerminal", exist.from.get, proto.edgeLabel, proto.destinationLabel, id.asInstanceOf[String], exist.properties)

              val edge = ProtoEdge(
                label = proto.edgeLabel,
                fromLabel = exist.fromLabel.get,
                toLabel = proto.destinationLabel,
                from = exist.from.get,
                to = id.asInstanceOf[String],
                properties = (exist.properties ++ data ++ lifted) -- proto.liftFields
              )

              emit.emitEdge(edge)
            }
          }
        }
      }
    }

    Map[String, Any]()
  }

  def innerVertex(emit: ProtographEmitter) (proto: InnerVertex) (vertex: ProtoVertex) (field: Option[Any]): Map[String, Any] = {
    def extract(nest: Map[String, Any]) {
      val embedded = nest + (proto.outerId -> vertex.gid)
      val inner = processVertex(emit) (proto.destinationLabel) (embedded)
      val edge = ProtoEdge(
        label = proto.edgeLabel,
        fromLabel = vertex.label,
        toLabel = proto.destinationLabel,
        from = vertex.gid,
        to = inner.gid
      )

      emit.emitVertex(vertex)
      emit.emitEdge(edge)
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
    }.getOrElse(Map[String, Any]())
  }

  def serializeField(map: SerializeField) (field: Option[Any]): Map[String, Any] = {
    field.map { inner =>
      val json = JsonIO.write(inner)
      Map[String, Any](map.serializedName -> json)
    }.getOrElse(Map[String, Any]())
  }

  def spliceMap(map: SpliceMap) (field: Option[Any]): Map[String, Any] = {
    field.map { inner =>
      inner.asInstanceOf[Map[String, Any]].map { pair =>
        val key = map.prefix + "." + pair._1
        (key -> pair._2)
      }
    }.getOrElse(Map[String, Any]())
  }

  def joinList(list: JoinList) (key: String) (field: Option[Any]): Map[String, Any] = {
    field.map { inner =>
      val join = inner.asInstanceOf[List[Any]].map(_.toString).mkString(list.delimiter)
      Map[String, Any](key -> join)
    }.getOrElse(Map[String, Any]())
  }

  def storeField(store: StoreField) (key: String) (field: Option[Any]): Map[String, Any] = {
    if (store.store) {
      field.map { inner =>
        Map[String, Any](key -> inner)
      }.getOrElse(Map[String, Any]())
    } else {
      Map[String, Any]()
    }
  }

  def processEdge(emit: ProtographEmitter) (label: String) (data: Map[String, Any]): Unit = {
    val transform = transformFor(label)
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

  def processVertex(emit: ProtographEmitter) (label: String) (data: Map[String, Any]): ProtoVertex = {
    val transform = transformFor(label)
    val gid = transform.gid(data)
    val vertex = ProtoVertex(
      label = label,
      gid = gid
    )

    val properties = transform.transform.actions.foldLeft(Map[String, Any]()) { (outcome, action) =>
      val key = action.field
      val field = data.get(key)
      val properties = action.action match {
        case Action.RemoteEdges(remote) =>
          associateEdges(emit) (remote) (vertex) (field)
        case Action.LinkThrough(link) =>
          linkThrough(emit) (link) (vertex) (field)
        case Action.InnerVertex(inner) =>
          innerVertex(emit) (inner) (vertex) (field)
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

      outcome ++ properties
    } + ("gid" -> gid)

    val remaining = transform.transform.actions.map(_.field).foldLeft(data) ((data, field) =>
      data - field
    )

    emit.emitVertex(vertex.copy(properties = properties ++ remaining))
    vertex
  }

  def processMessage(emit: ProtographEmitter) (label: String) (data: Map[String, Any]): Unit = {
    transformFor(label).transform.role match {
      case "Vertex" => processVertex(emit) (label) (data)
      case "Edge" => processEdge(emit) (label) (data)
    }
  }
}

class CamelCaseSerializer extends JsonSerializer[String] {
  def capitalize(s: String): String = {
    Character.toUpperCase(s.charAt(0)) + s.substring(1)
  }

  def camelize(s: String): String = {
    val parts = s.split("_").toList
    (parts.head :: parts.tail.map(capitalize)).mkString
  }

  def serialize(value: String, gen: JsonGenerator, serializers: SerializerProvider) {
    gen.writeFieldName(camelize(value))
  }
}

object Protograph {
  val simpleModule: SimpleModule = new SimpleModule();
  simpleModule.addKeySerializer(classOf[String], new CamelCaseSerializer());

  val mapper = new ObjectMapper()
  mapper.registerModule(simpleModule);

  def parseJSON(message: String): TransformMessage = {
    JsonFormat.fromJsonString[TransformMessage](message)
  }

  def load(path: String): List[TransformMessage] = {
    val yaml = new Yaml()
    val obj = yaml.load(new FileInputStream(path)).asInstanceOf[java.util.ArrayList[Any]]
    obj.asScala.toList.map { step =>
      val json = mapper.writeValueAsString(step)
      println(json)
      parseJSON(json)
    }
  }

  def loadProtograph(path: String): Protograph = {
    val transforms = load(path)
    new Protograph(transforms)
  }
}
