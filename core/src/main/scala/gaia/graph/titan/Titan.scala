package gaia.graph.titan

// import gaia.config._
// import gaia.graph._
// import gaia.schema.GaiaSchema

// import scala.util.Try
// import gremlin.scala._

// import org.apache.tinkerpop.gremlin.structure.Graph
// import org.apache.commons.configuration.BaseConfiguration
// import com.thinkaurelius.titan.core.{TitanFactory, TitanGraph}
// import com.thinkaurelius.titan.core.util.TitanCleanup

// class GaiaTitan(config: GaiaGraphConfig) extends GaiaGraph {
//   def connect(): TitanGraph = {
//     val base = new BaseConfiguration()
//     base.setProperty("storage.backend", "cassandra")
//     base.setProperty("storage.hostname", config.host.getOrElse("localhost"))
//     base.setProperty("storage.cassandra.keyspace", config.keyspace.getOrElse("gaia"))
//     // base.setProperty("query.batch", "true")
//     // base.setProperty("storage.cassandra.frame-size-mb", "400")

//     TitanFactory.open(base)
//   }

//   lazy val connection = connect()
//   def graph(): Graph = {
//     connection
//   }

//   lazy val persistedSchema: GaiaSchema = GaiaSchema.load(config.protograph.getOrElse("default"))
//   def schema: GaiaSchema = {
//     persistedSchema
//   }

//   def retryCommit(times: Integer): Unit = {
//     if (times == 0) {
//       println("TRANSACTION FAILED!")
//     } else {
//       try {
//         graph.asInstanceOf[TitanGraph].tx.commit()
//       } catch {
//         case ex: Exception => {
//           retryCommit(times - 1)
//         }
//       }
//     }
//   }

//   def commit(): Unit = {
//     retryCommit(5)
//   }

//   def makeIndex(name: String) (keys: Map[String, Class[_]]): Try[Unit] = {
//     Try {
//       val mg = graph.asInstanceOf[TitanGraph].openManagement()
//       val preindex = mg.buildIndex(name, classOf[Vertex])
//       val index = keys.foldLeft(preindex) {(index, kv) =>
//         val (s, c) = kv
//         val property = if (mg.getPropertyKey(s) == null) {
//           mg.makePropertyKey(s).dataType(c).make()
//         } else {
//           mg.getPropertyKey(s)
//         }

//         index.addKey(property)
//       }

//       index.buildCompositeIndex()
//       mg.commit()
//     }
//   }

//   def makeIndexes(spec: Map[String, Map[String, Class[_]]]): Try[Unit] = {
//     Try {
//       for (kv <- spec) {
//         val (name, properties) = kv
//         makeIndex(name) (properties)
//       }
//     }
//   }
// }
