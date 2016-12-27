package gaia.ingest

import gaia.graph._
import gaia.schema._
import gremlin.scala._

import scala.io.Source
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization.{write}

import java.lang.{Long => Llong}
import java.io.File

import scala.collection.JavaConversions._

case class GraphIngestor(graph: GaiaGraph) extends GaiaIngestor {
  val edgesPattern = "(.*)Edges$".r
  val propertiesPattern = "(.*)Properties$".r
  val keymap = collection.mutable.Map[String, Key[Any]]()
  implicit val formats = DefaultFormats

  def findKey[T](key: String): Key[T] = {
    val newkey = keymap.get(key).getOrElse {
      val newkey = Key[Any](key)
      keymap(key) = newkey
      newkey
    }

    newkey.asInstanceOf[Key[T]]
  }

  def stringFor(obj: JObject) (key: String): String = {
    (obj \\ key).asInstanceOf[JString].s
  }

  def camelize(s: String): String = {
    val break = s.split("_")
    val upper = break.head +: break.tail.map(_.capitalize)
    upper.mkString("")
  }

  def uncapitalize(s: String): String = {
    if (s.size > 0) {
      val c = s.toCharArray
      c(0) = Character.toLowerCase(c(0))
      new String(c)
    } else {
      ""
    }
  }

  def findVertex(graph: GaiaGraph) (label: String) (gid: String): Vertex = {
    val vertex = graph.namedVertex(label) (gid)
    graph.associateType(vertex) (label)
    vertex
  }

  def setProperty(vertex: Vertex) (field: Tuple2[String, JValue]): Unit = {
    val key = camelize(field._1)
    field._2 match {
      case JString(value) =>
        vertex.setProperty(findKey[String](key), value)
      case JDouble(value) =>
        vertex.setProperty(findKey[Double](key), value)
      case JBool(value) =>
        vertex.setProperty(findKey[Boolean](key), value)
      case JInt(value) =>
        vertex.setProperty(findKey[Long](key), value.toLong)
      case _ =>
        println("Unsupported Key: " + key)
    }
  }

  def setProperties(vertex: Vertex) (prefix: String) (fields: List[Tuple2[String, JValue]]): Unit = {
    for (field <- fields) {
      setProperty(vertex) ((prefix + "." + field._1, field._2))
    }
  }

  def ingestVertex(json: JValue): Vertex = {
    val data = json.asInstanceOf[JObject]
    val gid = stringFor(data) ("gid")
    val label = uncapitalize(stringFor(data) ("type"))
    val vertex = findVertex(graph) (label) (gid)

    for (field <- data.obj) {
      val key = field._1
      field._2 match {
        case JObject(obj) =>
          propertiesPattern.findFirstMatchIn(key).map(_.subgroups) match {
            case None => {
              val serial = write(JObject(obj))
              setProperty(vertex) ((key, new JString(serial)))
            }
            case Some(matches) => setProperties(vertex) (matches.head) (obj)
          }
        case JArray(arr) =>
          edgesPattern.findAllIn(key).matchData.foreach { edgeMatch =>
            for (value <- arr) {
              val edge = value.asInstanceOf[JString].s
              val label = Gid.labelPrefix(edge)
              if (label != "") {
                graph.associateOut(vertex) (edgeMatch.group(1)) (label) (edge)
              }
            }
          }
        case _ =>
          setProperty(vertex) (field)
      }
    }

    graph.commit()
    vertex
  }

  def ingestMessage(message: String) {
    println(message)
    val json = parse(message)
    println(json)
    val vertex = ingestVertex(json)
  }
}

object GraphIngestor {
  def parseJson(raw: String): JValue = {
    parse(raw)
  }
}
