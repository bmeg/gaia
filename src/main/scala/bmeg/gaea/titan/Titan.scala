package bmeg.gaea.titan

import org.apache.commons.configuration.BaseConfiguration
import com.thinkaurelius.titan.core.{TitanFactory, TitanGraph}
import com.thinkaurelius.titan.core.util.TitanCleanup

object Titan {
  def configuration() : BaseConfiguration = {
    val conf = new BaseConfiguration()
    conf.setProperty("storage.backend", "cassandra")
    conf.setProperty("storage.hostname", "localhost")
    conf
  }

  def connect(conf : BaseConfiguration) : TitanGraph = {
    TitanFactory.open(conf)
  }
}
