package gaea.frame

import gremlin.scala._
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization.write

object Frame {
  // apparently you need to say this if you want to write JSON
  implicit val formats = DefaultFormats

  class FrameBuilder(header: List[String], data: List[List[String]], default: String, rowField: String, dataField: String) {
    def addVertex(vertex: Vertex): FrameBuilder = {
      val id = vertex.value[String](rowField)
      val values = hydrate(vertex) (dataField)
      val newKeys = values.keys.toSet.diff(header.toSet).toList
      val newHeader = header ++ newKeys
      val row = id :: newHeader.map(column => values.get(column).map(_.toString).getOrElse(default))
      val newData = row :: data

      new FrameBuilder(newHeader, newData, default, rowField, dataField)
    }

    def finish(): Seq[Seq[String]] = {
      val output = (rowField :: header) :: data
      val headerSize = output.head.size
      output.map { line =>
        line ++ List.fill(headerSize - line.size) (default)
      }
    }
  }

  def emptyBuilder(default: String) (rowField: String) (dataField: String): FrameBuilder = {
    new FrameBuilder(List[String](), List[List[String]](), default, rowField, dataField)
  }

  def hydrate(vertex: Vertex) (key: String): Map[String, Double] = {
    val raw = vertex.value[String](key)
    parse(raw).extract[Map[String, Double]]
  }

  def convertFrame(default: String) (vertexes: Seq[Vertex]) (rowField: String) (dataField: String): Seq[Seq[String]] = {
    vertexes.foldLeft(emptyBuilder(default) (rowField) (dataField)) { (frame, vertex) =>
      frame.addVertex(vertex)
    }.finish()
  }

  def renderTSV(data: Seq[Seq[String]]): String = {
    data.map { line =>
      line.mkString("\t")
    }.mkString("\n")
  }

  def renderFrame(default: String) (vertexes: Seq[Vertex]) (rowField: String) (dataField: String): String = {
    val frame = convertFrame(default) (vertexes) (rowField) (dataField)
    renderTSV(frame)
  }
}
