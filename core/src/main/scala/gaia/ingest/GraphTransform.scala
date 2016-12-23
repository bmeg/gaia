package gaia.ingest

import gaia.graph._
import gaia.io.JsonIO
import gremlin.scala._

import scala.collection.JavaConversions._

case class GraphTransform(graph: GaiaGraph, protoGrapher: ProtoGrapher) extends MessageTransform {
  val edgesPattern = "(.*)Edges$".r
  val propertiesPattern = "(.*)Properties$".r
  val keymap = collection.mutable.Map[String, Key[Any]]()

  def findKey[T](key: String): Key[T] = {
    val newkey = keymap.get(key).getOrElse {
      val newkey = Key[Any](key)
      keymap(key) = newkey
      newkey
    }

    newkey.asInstanceOf[Key[T]]
  }

  def stringFor(obj: Map[String,Any]) (key: String): String = {
    (obj.get(key).get).asInstanceOf[String]
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

  def setProperty(vertex: Vertex) (field: Tuple2[String, Any]): Unit = {
    val key = camelize(field._1)
    field._2 match {
      case value:String =>
        vertex.setProperty(findKey[String](key), value)
      case value:Double =>
        vertex.setProperty(findKey[Double](key), value)
      case value:Boolean =>
        vertex.setProperty(findKey[Boolean](key), value)
      case value:Int =>
        vertex.setProperty(findKey[Long](key), value.toLong)
      case value:List[Any] =>
        vertex.setProperty(findKey[String](key), JsonIO.writeList(value)  )
      case _ =>
        println("Unsupported Key: " + key, field._2)
    }
  }

  def setProperties(vertex: Vertex) (prefix: String) (fields: List[Tuple2[String, Any]]): Unit = {
    for (field <- fields) {
      setProperty(vertex) ((prefix + "." + field._1, field._2))
    }
  }

  def ingestVertex(data: Map[String,Any]): Vertex = {

    val typeString = stringFor(data)("#type")
    val c = protoGrapher.getConverter(typeString)

    //val data = json.asInstanceOf[JObject]
    val gid = c.getGID(data)
    if (gid == null) {
      throw new TransformException("Unable to create GID")
    }

    if (typeString == null) {
      throw new TransformException("Untyped Message")
    }
    val label = uncapitalize(typeString)
    val vertex = findVertex(graph) (label) (gid)

    for (field <- data.iterator) {
      val key = field._1
      field._2 match {
        case obj:Map[String,Any] =>
          propertiesPattern.findFirstMatchIn(key).map(_.subgroups) match {
            case None => {
              setProperty(vertex) ((key, JsonIO.writeMap(obj)))
            }
            case Some(matches) => setProperties(vertex) (matches.head) (obj.toList)
          }
        case arr:List[Any] =>
          edgesPattern.findAllIn(key).matchData.foreach { edgeMatch =>
            for (value <- arr) {
              val edge = value.asInstanceOf[String]
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

  def ingestMessage(message: Map[String,Any]) {
    val vertex = ingestVertex(message)
  }

}

