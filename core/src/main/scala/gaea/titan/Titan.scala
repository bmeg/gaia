package gaea.titan

import gaea.graph._

// import org.apache.tinkerpop.gremlin.structure.Graph
// import org.apache.commons.configuration.BaseConfiguration
// import com.thinkaurelius.titan.core.{TitanFactory, TitanGraph}
// import com.thinkaurelius.titan.core.util.TitanCleanup

// class GaeaTitan(config: GaeaGraphConfig) extends GaeaGraph {
//   def connect(): TitanGraph = {
//     val base = new BaseConfiguration()
//     base.setProperty("storage.backend", "cassandra")
//     base.setProperty("storage.hostname", config.host)
//     base.setProperty("storage.cassandra.keyspace", config.keyspace)

//     TitanFactory.open(base)
//   }

//   lazy val connection = connect()

//   def graph(): Graph = {
//     connection
//   }
// }
