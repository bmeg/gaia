package gaia.ingest

/**
  * Created by ellrott on 12/18/16.
  */

import gaia.io.JsonIO
import gaia.schema.Protograph
import gaia.schema.Protograph.{FieldAction, ProcessMessage}

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
// case class MessageConverter(label: String, gidFormat: String, processors: List[])

class ProtographMessageParser(val convert: ProcessMessage) {
  def gid(message: Map[String,Any]): String = {
    // if (convert == null) {
    //   return "gid"
    // }
    // if (convert.getGidFormat.getFieldSelection == null) {
    //   return "gid"
    // }

    println(message.toString)
    message.get(convert.gidFormat).get.asInstanceOf[String]
  }

  /// List out the edge creation requests
  /// These provide the query format, which needs to be searched on the graph to
  /// find unique matches to determine the destination vertex for the edges to be
  /// created
  def destinations(): Seq[MessageVertexQuery] = {
    // if (convert == null) {
    //   return Iterator[MessageVertexQuery]()
    // }

    convert.process.filter(_.action.isSingleEdge).map { step => 
      val edge = step.action.singleEdge.get
      new MessageVertexQuery(step.name, edge.edgeLabel, edge.destinationLabel)
    }
  }

  /// Create additional vertices that encoded inside of the message
  def children(): Seq[MessageVertexQuery] = {
    // if (convert == null) {
    //   return Iterator[MessageVertexQuery]()
    // }

    convert.process.filter(_.action.isNestedVertex).map { step =>
      val edge = step.action.nestedVertex.get
      new MessageVertexQuery(step.name, edge.edgeLabel, edge.destinationLabel)
    }
  }

  // /// For a given field name, determine the action to be taken
  // def fieldActionFor(name: String): FieldAction.Action = {
  //   // if (convert == null) return FieldAction.NOTHING
  //   if (name == "#type") {
  //     FieldAction.Action.Ignore("nothing")
  //   } else {
  //     val actions = convert.process.filter(_.name == name)
  //     if (actions.size == 0) {
  //       FieldAction.Action.Store("everything")
  //     } else {
  //       actions.head.action
  //     }
  //   }
  // }
}

class Protographer(converters: Seq[ProcessMessage]) {
  val converterMap = converters.map(step => (step.label, step)).toMap

  def converterFor(typ: String): ProtographMessageParser = {
    new ProtographMessageParser(converterMap.getOrElse(typ, null))
  }
}

class CamelCaseSerializer extends JsonSerializer[String] {
  def capitalize(s: String): String = {
    Character.toUpperCase(s.charAt(0)) + s.substring(1)
  }

  def serialize(value: String, gen: JsonGenerator, serializers: SerializerProvider) {
    val parts = value.split("_").toList
    val key = (parts.head :: parts.tail.map(capitalize)).mkString
    gen.writeFieldName(key)
  }
}

object Protographer {
  val simpleModule: SimpleModule = new SimpleModule();
  simpleModule.addKeySerializer(classOf[String], new CamelCaseSerializer());

  val mapper = new ObjectMapper()
  mapper.registerModule(simpleModule);

  def parseJSON(message: String): ProcessMessage = {
    JsonFormat.fromJsonString[ProcessMessage](message)
    // val b = ProcessMessage.newBuilder()
    // val parser = JsonFormat.parser().ignoringUnknownFields()
    // parser.merge(message, b)
    // b.build()
  }

  def load(path: String): List[ProcessMessage] = {
    val yaml = new Yaml()
    val obj = yaml.load(new FileInputStream(path)).asInstanceOf[java.util.ArrayList[Any]]
    obj.asScala.toList.map { step =>
      val json = mapper.writeValueAsString(step)
      println(json)
      parseJSON(json)
    }
  }

  def loadProtograph(path: String): Protographer = {
    val process = load(path)
    new Protographer(process)
  }
}
