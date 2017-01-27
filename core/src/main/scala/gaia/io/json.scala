package gaia.io

import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.databind.ObjectMapper
import scala.collection.mutable

object JsonIO {
  val mapper = new ObjectMapper()
  mapper.registerModule(DefaultScalaModule)

  def readMap(text: String): Map[String,Any] = {
    mapper.readValue(text, classOf[Map[String,Any]] )
  }

  def readList(text: String): List[Any] = {
    mapper.readValue(text, classOf[List[Any]] )
  }

  def writeMap(message: Map[String,Any]): String = {
    mapper.writeValueAsString(message)
  }

  def writeList(message: List[Any]): String = {
    mapper.writeValueAsString(message)
  }
}
