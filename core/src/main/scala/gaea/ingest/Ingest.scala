package gaea.ingest

import gaea.titan.Titan

import gremlin.scala._

import java.lang.{Long => Llong}
import java.io.File
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization.write
import com.thinkaurelius.titan.core.TitanGraph

object Ingest {
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

  def listFiles(dir: String): List[File] = {
    val d = new File(dir)
    if (d.exists) {
      if (d.isDirectory) {
        d.listFiles.filter(_.isFile).toList
      } else {
        List[File](d)
      }
    } else {
      List[File]()
    }
  }

  def parseJson(raw: String): JValue = {
    parse(raw)
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

  def retryCommit(graph: TitanGraph) (times: Integer): Unit = {
    if (times == 0) {
      println("TRANSACTION FAILED!")
    } else {
      try {
        graph.tx.commit()
      } catch {
        case ex: Exception => {
          retryCommit(graph) (times - 1)
        }
      }
    }
  }

  def findVertex(graph: TitanGraph) (label: String) (gid: String): Vertex = {
    val vertex = Titan.namedVertex(graph) (label) (gid)
    Titan.associateType(graph) (vertex) (label)
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

  def ingestVertex(graph: TitanGraph) (json: JValue): Vertex = {
    val data = json.asInstanceOf[JObject]
    val gid = stringFor(data) ("gid")
    val label = uncapitalize(stringFor(data) ("type"))
    val vertex = findVertex(graph) (label) (gid)

    for (field <- data.obj) {
      val key = field._1
      field._2 match {
        case JObject(obj) =>
          propertiesPattern.findFirstMatchIn(key).map(_.subgroups) match {
            case None => setProperty(vertex) ((key, new JString(write(obj))))
            case Some(matches) => setProperties(vertex) (matches.head) (obj)
          }
        case JArray(arr) =>
          edgesPattern.findAllIn(key).matchData.foreach { edgeMatch =>
            for (value <- arr) {
              val edge = value.asInstanceOf[JString].s
              val label = Titan.labelPrefix(edge)
              if (label != "") {
                Titan.associateOut(graph) (vertex) (edgeMatch.group(1)) (label) (edge)
              }
            }
          }
        case _ =>
          setProperty(vertex) (field)
      }
    }

    retryCommit(graph) (5)
    vertex
  }
}
