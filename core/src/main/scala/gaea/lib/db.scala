package gaea.lib

import org.apache.commons.configuration.BaseConfiguration
import com.thinkaurelius.titan.core.{TitanFactory, TitanGraph}
import com.thinkaurelius.titan.core.util.TitanCleanup
import org.apache.tinkerpop.gremlin.structure.Graph
import gremlin.scala._

class GraphConnection(hostname: String) {
  val config = new BaseConfiguration()
  config.setProperty("storage.backend", "cassandra")
  config.setProperty("storage.hostname", hostname)

  def connect(): Graph = {
    TitanFactory.open(config)
  }
}