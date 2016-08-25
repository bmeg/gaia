package gaea.titan

import org.apache.commons.configuration.BaseConfiguration
import com.thinkaurelius.titan.core.{TitanFactory, TitanGraph}
import com.thinkaurelius.titan.core.util.TitanCleanup
import gremlin.scala._

object Titan {
  val Gid = Key[String]("gid")

  def configuration(properties: Map[String, String]): BaseConfiguration = {
    val config = new BaseConfiguration()
    config.setProperty("storage.backend", "cassandra")
    config.setProperty("storage.hostname", "localhost")
    config.setProperty("storage.cassandra.keyspace", "gaea")

    for (property <- properties) {
      val (key, value) = property
      config.setProperty(key, value)
    }

    config
  }

  def connect(conf: BaseConfiguration): TitanGraph = {
    TitanFactory.open(conf)
  }

  def defaultGraph(): TitanGraph = {
    connect(configuration(Map[String, String]()))
  }

  lazy val connection = defaultGraph()

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

  def findVertex[A](graph: TitanGraph) (label: String) (keys: Map[Key[A], A]): Option[Vertex] = {
    val prequery = graph.V.hasLabel(label)
    val query = keys.foldLeft(prequery) {(query, kv) =>
      val (key, value) = kv
      query.has(key, value)
    }

    query.toList.headOption
  }

  def namedVertex(graph: TitanGraph) (label: String) (gid: String): Vertex = {
    graph.V.has(Gid, gid).headOption.getOrElse {
      graph + (label, Gid -> gid)
    }
  }

  def typeQuery(graph: TitanGraph) (typ: String): GremlinScala[Vertex, shapeless.HNil] = {
    graph.V.hasLabel("type").has(Gid, "type:" + typ).out("hasInstance")
  }

  def typeVertexes(graph: TitanGraph) (typ: String): List[Vertex] = {
    typeQuery(graph) (typ).toList
  }

  def associateOut(graph: TitanGraph) (from: Vertex) (edge: String) (toLabel: String) (toGid: String): Boolean = {
    if (from.out(edge).has(Gid, toGid).toList.isEmpty) {
      val to = namedVertex(graph) (toLabel) (toGid)
      from --- (edge) --> to
      true
    } else {
      false
    }
  }

  def associateIn(graph: TitanGraph) (from: Vertex) (edge: String) (toLabel: String) (toGid: String): Boolean = {
    if (from.in(edge).has(Gid, toGid).toList.isEmpty) {
      val to = namedVertex(graph) (toLabel) (toGid)
      from <-- (edge) --- to
      true
    } else {
      false
    }
  }

  def associateType(graph: TitanGraph) (instance: Vertex) (typ: String): Boolean = {
    associateIn(graph) (instance) ("hasInstance") ("type") ("type:" + typ)
  }

  def makeIndex(graph: TitanGraph) (name: String) (keys: Map[String, Class[_]]) = {
    val mg = graph.openManagement()
    val preindex = mg.buildIndex(name, classOf[Vertex])
    val index = keys.foldLeft(preindex) {(index, kv) =>
      val (s, c) = kv
      val property = if (mg.getPropertyKey(s) == null) {
        mg.makePropertyKey(s).dataType(c).make()
      } else {
        mg.getPropertyKey(s)
      }

      index.addKey(property)
    }

    index.buildCompositeIndex()
    mg.commit()
  }

  def makeIndexes(graph: TitanGraph) (spec: Map[String, Map[String, Class[_]]]): Unit = {
    for (kv <- spec) {
      val (name, properties) = kv
      makeIndex(graph) (name) (properties)
    }
  }
}
