package gaea.frame

import gaea.titan.Titan

import gremlin.scala._
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization.write

import com.thinkaurelius.titan.core.TitanGraph
import java.lang.{Long => Llong}
import java.io.File

object Frame {
  implicit val formats = DefaultFormats

  class FrameBuilder(header: List[String], data: List[List[String]]) {
    def addVertex(default: String) (rowField: String) (dataField: String) (vertex: Vertex): FrameBuilder = {
      val id = vertex.value[String](rowField)
      val values = hydrate(vertex) (dataField)
      val newKeys = header.toSet.diff(values.keys.toSet).toList
      val newHeader = header ++ newKeys
      val row = id +: header.drop(1).map(column => values.get(column).map(_.toString).getOrElse(default))
      val newData = row +: data
      new FrameBuilder(newHeader, newData)
    }

    def finish(): Seq[Seq[String]] = {
      header +: data
    }
  }

  def emptyBuilder(): FrameBuilder = {
    new FrameBuilder(List[String]("id"), List[List[String]]())
  }

  def hydrate(vertex: Vertex) (key: String): Map[String, Double] = {
    val raw = vertex.value[String](key)
    parse(raw).extract[Map[String, Double]]
  }

  def convertFrame(default: String) (vertexes: Seq[Vertex]) (rowField: String) (dataField: String): Seq[Seq[String]] = {
    vertexes.foldLeft(emptyBuilder()) { (frame, vertex) =>
      frame.addVertex(default) (rowField) (dataField) (vertex)
    }.finish()
  }

  def renderTSV(default: String) (data: Seq[Seq[String]]): String = {
    val headerSize = data.head.size
    data.map { line =>
      val fullLine = line ++ List.fill(headerSize - line.size) (default)
      val joined = fullLine.mkString("\t")
    }.mkString("\n")
  }

  def renderFrame(default: String) (vertexes: Seq[Vertex]) (rowField: String) (dataField: String): String = {
    val frame = convertFrame(default) (vertexes) (rowField) (dataField)
    renderTSV(default) (frame)
  }
}
