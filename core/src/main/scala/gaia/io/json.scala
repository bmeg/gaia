package gaia.io


import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}

import scala.collection.mutable

class JsonIO {
  val mapper = new ObjectMapper()

  def ReadMap(text: String) : java.util.Map[String,Object] = {
    val o = mapper.readValue(text, classOf[java.util.Map[String,Object]] )
    return o
  }

  def WriteMap(message: java.util.Map[String,Object]) : String = {
    val json = mapper.writeValueAsString(message)
    return json
  }

}
