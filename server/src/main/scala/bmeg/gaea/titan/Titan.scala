package bmeg.gaea.titan

import org.apache.commons.configuration.BaseConfiguration
import com.thinkaurelius.titan.core.{TitanFactory, TitanGraph}
import com.thinkaurelius.titan.core.util.TitanCleanup
import gremlin.scala._

object Titan {
  def configuration(): BaseConfiguration = {
    val config = new BaseConfiguration()
    config.setProperty("storage.backend", "cassandra")
    config.setProperty("storage.hostname", "localhost")
    config
  }

  def connect(conf: BaseConfiguration): TitanGraph = {
    TitanFactory.open(conf)
  }

  def findVertex[A](graph: TitanGraph) (label: String) (key: Key[A]) (value: A): Option[Vertex] = {
    graph.V.has(label, key, value).toList.headOption
  }
}
