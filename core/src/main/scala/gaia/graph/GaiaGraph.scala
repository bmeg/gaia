package gaia.graph

import gaia.schema._

import gremlin.scala._
import scala.util.Try
import org.apache.tinkerpop.gremlin.structure.Graph

object Gid extends Key[String]("gid") {
  def labelPrefix(gid: String): String = {
    val split = gid.split(":")
    if (split.size == 1) {
      ""
    } else {
      split(0)
    }
  }

  def removePrefix(gid: String): String = {
    val split = gid.split(":")
    if (split.size == 1) {
      split(0)
    } else {
      split.drop(1).reduceLeft((t, s) => t + ":" + s)
    }
  }
}

trait GaiaGraph {
  def graph(): Graph
  def schema(): GaiaSchema

  def makeIndex(name: String) (keys: Map[String, Class[_]]): Try[Unit]
  def makeIndexes(spec: Map[String, Map[String, Class[_]]]): Try[Unit]
  def commit(): Unit

  val Symbol = Key[String]("symbol")

  def V(): GremlinScala[Vertex,shapeless.HNil] = {
    graph.V
  }

  def findVertex[A](label: String) (keys: Map[Key[A], A]): Option[Vertex] = {
    val prequery = graph.V.hasLabel(label)
    val query = keys.foldLeft(prequery) {(query, kv) =>
      val (key, value) = kv
      query.has(key, value)
    }

    query.toList.headOption
  }

  def namedVertex(label: String) (gid: String): Vertex = {
    graph.V.has(Gid, gid).headOption.getOrElse {
      graph + (label, Gid -> gid)
    }
  }

  def typeQuery(typ: String): GremlinScala[Vertex, shapeless.HNil] = {
    graph.V.hasLabel("type").has(Gid, "type:" + typ).out("hasInstance")
  }

  def typeVertexes(typ: String): List[Vertex] = {
    typeQuery(typ).toList
  }

  def associateOut(from: Vertex) (edge: String) (toLabel: String) (toGid: String): Boolean = {
    if (from.out(edge).has(Gid, toGid).toList.isEmpty) {
      val to = namedVertex(toLabel) (toGid)
      from --- (edge) --> to
      true
    } else {
      false
    }
  }

  def associateIn(from: Vertex) (edge: String) (toLabel: String) (toGid: String): Boolean = {
    if (from.in(edge).has(Gid, toGid).toList.isEmpty) {
      val to = namedVertex(toLabel) (toGid)
      from <-- (edge) --- to
      true
    } else {
      false
    }
  }

  def associateType(instance: Vertex) (typ: String): Unit = {
    val tid = "type:" + typ
    if (instance.in("hasInstance").has(Gid, tid).toList.isEmpty) {
      val typeVertex = graph.V.has(Gid, tid).headOption.getOrElse {
        val vertex = graph + ("type", Gid -> tid, Symbol -> typ)
        val hub = graph.V.has(Gid, "type:type").headOption.getOrElse {
          graph + ("type", Gid -> "type:type", Symbol -> "type")
        }

        vertex <-- ("hasInstance") --- hub
        vertex
      }

      instance <-- ("hasInstance") --- typeVertex
    }
  }
}
