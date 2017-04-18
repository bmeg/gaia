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

case class Protograph(transforms: Seq[TransformMessage]) {
  val transformMap = transforms.map(step => (step.label, ProtographTransform.toProtograph(step))).toMap
  val default = TransformMessage(label = "default", gid = "default:{{gid}}")
  val defaultTransform = ProtographTransform.toProtograph(default)

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
