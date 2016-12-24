package gaia.ingest

import gaia.graph._
import gaia.io.JsonIO
import gaia.schema.ProtoGraph.FieldAction
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
    //FIXME: for now, determining the message type is a bit hard coded
    val typeString = stringFor(data)("#type")
    if (typeString == null) {
      throw new TransformException("Untyped Message")
    }

    //Get the ProtoGrapher instance for this message type
    //It will be responsible for reading the protograph message and
    //determining which operations will happen to the message
    val conv = protoGrapher.getConverter(typeString)

    //Determine the GID from the message
    val gid = conv.getGID(data)
    if (gid == null) {
      throw new TransformException("Unable to create GID")
    }

    //Start decorating the vertex
    val label = uncapitalize(typeString)
    val vertex = findVertex(graph) (label) (gid)

    //check the protograph description for edges that need to be created
    conv.getDestVertices().foreach( x => {
      val edge = x.edgeLabel
      val label = Gid.labelPrefix(edge)  //what edge label
      val query = data.get(x.queryField) //query the current message to determine how to find the dest vertex
      if (query.isDefined) {
        query.get match {
          case value: String =>
            //if we found a string, use it
            graph.associateOut(vertex)(value)(x.dstLabel)(edge)
          case value: List[String] =>
            value.foreach(y => {
              //if we found a list, cycle through each one and process
              graph.associateOut(vertex)(y)(x.dstLabel)(edge)
            })
        }
      }
    })

    //for each field in the message, determine what to do with it
    for (field <- data.iterator) {
      val key = field._1
      conv.getFieldAction(key) match {
        case FieldAction.STORE => setProperty(vertex) (field)
        case FieldAction.SERIALIZE =>
          field._2 match {
            case obj: Map[String, Any] =>
              setProperty(vertex)((key, JsonIO.writeMap(obj)))
            case arr: List[Any] =>
              setProperty(vertex)((key, JsonIO.writeList(arr)))
          }
        case _ =>
      }
    }
    graph.commit()
    vertex
  }

  def ingestMessage(message: Map[String,Any]) {
    val vertex = ingestVertex(message)
  }

}

