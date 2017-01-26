package gaia.ingest

/**
  * Created by ellrott on 12/18/16.
  */

import gaia.io.JsonIO
import gaia.schema.Protograph
import gaia.schema.Protograph.{FieldAction, TransformMessage}

import java.io.FileInputStream
// import com.google.protobuf.util.JsonFormat
import scala.collection.mutable
import collection.JavaConverters._

import org.yaml.snakeyaml.Yaml
// import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.fasterxml.jackson.core.{JsonGenerator}
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper, JsonSerializer, SerializerProvider}
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule

import com.trueaccord.scalapb.json.JsonFormat

case class MessageVertexQuery(queryField: String, edgeLabel: String, destinationLabel: String)
// case class FieldProcessor(name: String)
// case class MessageConverter(label: String, gid: String, processors: List[])

class ProtographMessageParser(val transform: TransformMessage) {
  def gid(message: Map[String,Any]): String = {
    // if (convert == null) {
    //   return "gid"
    // }
    // if (convert.getGidFormat.getFieldSelection == null) {
    //   return "gid"
    // }

    println(message.toString)
    message.get(transform.gid).get.asInstanceOf[String]
  }

  /// List out the edge creation requests
  /// These provide the query format, which needs to be searched on the graph to
  /// find unique matches to determine the destination vertex for the edges to be
  /// created
  def destinations(): Seq[MessageVertexQuery] = {
    // if (convert == null) {
    //   return Iterator[MessageVertexQuery]()
    // }

    transform.actions.filter(_.action.isSingleEdge).map { step => 
      val edge = step.action.singleEdge.get
      new MessageVertexQuery(step.field, edge.edgeLabel, edge.destinationLabel)
    }
  }

  /// Create additional vertices that encoded inside of the message
  def children(): Seq[MessageVertexQuery] = {
    // if (convert == null) {
    //   return Iterator[MessageVertexQuery]()
    // }

    transform.actions.filter(_.action.isInnerVertex).map { step =>
      val edge = step.action.innerVertex.get
      new MessageVertexQuery(step.field, edge.edgeLabel, edge.destinationLabel)
    }
  }

  // /// For a given field name, determine the action to be taken
  // def fieldActionFor(name: String): FieldAction.Action = {
  //   // if (convert == null) return FieldAction.NOTHING
  //   if (name == "#type") {
  //     FieldAction.Action.Ignore("nothing")
  //   } else {
  //     val actions = convert.transform.filter(_.name == name)
  //     if (actions.size == 0) {
  //       FieldAction.Action.Store("everything")
  //     } else {
  //       actions.head.action
  //     }
  //   }
  // }
}

class Protographer(transforms: Seq[TransformMessage]) {
  val transformMap = transforms.map(step => (step.label, step)).toMap
  val defaultTransform = TransformMessage(label = "default", gid = "default:{gid}")

  def converterFor(label: String): ProtographMessageParser = {
    new ProtographMessageParser(transformMap.getOrElse(label, null))
  }

  def transformFor(label: String): TransformMessage = {
    transformMap.getOrElse(label, defaultTransform)
  }

  def gid(data: Map[String, Any]): String = {
    ""
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

object Protographer {
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

  def loadProtograph(path: String): Protographer = {
    val transforms = load(path)
    new Protographer(transforms)
  }
}
