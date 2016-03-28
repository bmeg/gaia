package bmeg.gaea.titan

import org.apache.commons.configuration.BaseConfiguration
import com.thinkaurelius.titan.core.{TitanFactory, TitanGraph}
import com.thinkaurelius.titan.core.util.TitanCleanup

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
}
