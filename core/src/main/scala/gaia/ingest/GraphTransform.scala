package gaia.ingest

import gaia.graph._
import gaia.schema._
import gaia.io.JsonIO
import gaia.schema.Protograph.FieldAction
import gremlin.scala._

import scala.collection.JavaConverters._

case class GraphTransform(graph: GaiaGraph) extends MessageTransform {
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
        vertex.setProperty(findKey[String](key), JsonIO.writeList(value))
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
    // FIXME: for now, determining the message type is a bit hard coded
    val typeString = stringFor(data)("#type")
    if (typeString == null) {
      throw new TransformException("Untyped Message")
    }

    // Get the Protographer instance for this message type
    // It will be responsible for reading the protograph message and
    // determining which operations will happen to the message
    val converter = graph.schema.protograph.converterFor(typeString)

    // Determine the GID from the message
    val gid = converter.gid(data)
    println("GID: " + gid)
    if (gid == null) {
      throw new TransformException("Unable to create GID")
    }

    // Start decorating the vertex
    val label = uncapitalize(typeString)
    val vertex = findVertex(graph) (label) (gid)

    // check the protograph description for edges that need to be created
    converter.destinations().foreach { destination =>
      // printf("Add edge: %s %s %s\n", destination.queryField, destination.edgeLabel, destination.destinationLabel)
      val edge = destination.edgeLabel
      val query = data.get(destination.queryField) // query the current message to determine how to find the dest vertex
      if (query.isDefined) {
        query.get match {
          case value: String =>
            // if we found a string, use it
            graph.associateOut(vertex) (edge) (destination.destinationLabel) (value)
          case value: List[String] =>
            value.foreach { y =>
              // printf("Adding Edge %s %s\n", y, edge )
              // if we found a list, cycle through each one and process
              graph.associateOut(vertex) (edge) (destination.destinationLabel) (y)
            }
        }
      }
    }

    converter.children().foreach( child => {
      // printf("Add Child Edge: %s %s %s\n", child.queryField, child.edgeLabel, child.destinationLabel)
      val query = data.get(child.queryField) // query the current message to determine how to find the dest vertex
      if (query.isDefined) {
        query.get match {
          case value: List[Map[String,Any]] =>
            value.foreach(y => {
              val u = y.updated("#type", child.destinationLabel)
              println("Ingest", u)
              val v = ingestVertex(u)
              println("Vertex", v.properties().asScala.mkString(","))
              // if we found a list, cycle through each one and process
              graph.associateOut(vertex) (child.edgeLabel) (child.destinationLabel) (v.property(Gid).value())
            })
          case _ =>
            println("Should probably do something here")
        }
      }
    })

    // // for each field in the message, determine what to do with it
    // for (field <- data.iterator) {
    //   val key = field._1
    //   converter.fieldActionFor(key) match {
    //     case FieldAction.STORE => setProperty(vertex) (field)
    //     case FieldAction.SERIALIZE =>
    //       field._2 match {
    //         case obj: Map[String, Any] =>
    //           setProperty(vertex)((key, JsonIO.writeMap(obj)))
    //         case arr: List[Any] =>
    //           setProperty(vertex)((key, JsonIO.writeList(arr)))
    //       }
    //     case _ =>
    //   }
    // }

    graph.commit()
    vertex
  }

  def transform(message: Map[String,Any]) {
    val vertex = ingestVertex(message)
  }
}

