package bmeg.gaea.titan

import org.apache.commons.configuration.BaseConfiguration
import com.thinkaurelius.titan.core.{TitanFactory, TitanGraph}
import com.thinkaurelius.titan.core.util.TitanCleanup
import gremlin.scala._

object Titan {
  def configuration(properties: Map[String, String]): BaseConfiguration = {
    val config = new BaseConfiguration()
    config.setProperty("storage.backend", "cassandra")
    config.setProperty("storage.hostname", "localhost")

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

  def findVertex[A](graph: TitanGraph) (label: String) (keys: Map[Key[A], A]): Option[Vertex] = {
    val prequery = graph.V.hasLabel(label)
    val query = keys.foldLeft(prequery) {(query, kv) =>
      val (key, value) = kv
      query.has(key, value)
    }

    query.toList.headOption
  }

  def makeIndex(graph: TitanGraph) (name: String) (keys: Map[String, Class[_]]) = {
    val mg = graph.openManagement()
    val preindex = mg.buildIndex(name, classOf[Vertex])
    val index = keys.foldLeft(preindex) {(index, kv) =>
      val (s, c) = kv
      val property = mg.makePropertyKey(s).dataType(c).make()
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
