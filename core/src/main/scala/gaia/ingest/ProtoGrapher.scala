package gaia.ingest

/**
  * Created by ellrott on 12/18/16.
  */

import java.io.FileInputStream

import com.google.protobuf.util.JsonFormat
import gaia.io.JsonIO
import gaia.schema.ProtoGraph
import gaia.schema.ProtoGraph.MessageConvert
import org.yaml.snakeyaml.Yaml

import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.databind.ObjectMapper
import scala.collection.mutable
import collection.JavaConverters._


class ProtoGrapher(val msgs: List[ProtoGraph.MessageConvert]) {

}

object ProtoGrapher {

  def parseJSON(message: String): ProtoGraph.MessageConvert = {
    val b = MessageConvert.newBuilder()
    val parser = JsonFormat.parser().ignoringUnknownFields()
    parser.merge(message, b)
    return b.build()
  }



  def load(path: String) : ProtoGrapher = {
    val mapper = new ObjectMapper()

    var yaml = new Yaml()
    var obj = yaml.load(new FileInputStream(path)).asInstanceOf[java.util.ArrayList[Any]]
    val mlist = obj.asScala.map( x => {
      var s = mapper.writeValueAsString(x)
      val l = parseJSON(s)
      l
    })
    new ProtoGrapher(mlist.toList)
  }


}
