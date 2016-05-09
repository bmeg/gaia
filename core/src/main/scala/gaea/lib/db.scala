package gaea.lib

import org.apache.commons.configuration.BaseConfiguration
import com.thinkaurelius.titan.core.{TitanFactory, TitanGraph}
import com.thinkaurelius.titan.core.util.TitanCleanup
import gremlin.scala._

class TitanConnection(hostname: String) {
  val config = new BaseConfiguration()
  config.setProperty("storage.backend", "cassandra")
  config.setProperty("storage.hostname", hostname)

  def connect(): TitanGraph = {
    TitanFactory.open(config)
  }
}