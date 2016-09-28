package gaea.io


import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}

import scala.collection.mutable

class JsonIO {
  val mapper = new ObjectMapper()

  def ReadMap(text: String) : java.util.Map[Object,Object] = {
    val o = mapper.readValue(text, classOf[java.util.Map[Object,Object]] )
    return o
  }

  def WriteMap(message: java.util.Map[Object,Object]) : String = {
    val json = mapper.writeValueAsString(message)
    return json
  }

}