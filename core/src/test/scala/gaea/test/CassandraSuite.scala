
package gaea.test

import java.io.File

//import org.apache.cassandra.service.CassandraDaemon

import org.cassandraunit.utils.EmbeddedCassandraServerHelper
import org.scalatest.{BeforeAndAfter, FunSuite}


class CassandraSuite extends FunSuite with BeforeAndAfter {


  var CASSANDRA_HOST : String = null
  var CASSANDRA_CLUSTER : String = null
  before {
    EmbeddedCassandraServerHelper.startEmbeddedCassandra("cu-cassandra.yaml")
    CASSANDRA_HOST = EmbeddedCassandraServerHelper.getHost() + ":" + EmbeddedCassandraServerHelper.getRpcPort()
    CASSANDRA_CLUSTER = EmbeddedCassandraServerHelper.getClusterName()
  }

  after {

  }

/*

  before {
    System.setProperty("storage-config", "conf")
    val d = new CassandraDaemon()
    d.init(null)
    d.start()
  }
*/



}