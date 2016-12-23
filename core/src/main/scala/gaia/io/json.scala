package gaia.io


import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.databind.ObjectMapper
import scala.collection.mutable

class JsonIO {
  val mapper = new ObjectMapper()
  mapper.registerModule(DefaultScalaModule)

  def ReadMap(text: String) : Map[String,Any] = {
    val o = mapper.readValue(text, classOf[Map[String,Any]] )
    return o
  }

  def WriteMap(message: Map[String,Any]) : String = {
    val json = mapper.writeValueAsString(message)
    return json
  }

}
