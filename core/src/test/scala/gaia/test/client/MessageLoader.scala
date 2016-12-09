
package gaia.test.client


import java.io.File

import gaia.client.{ConnectionConfig, GaiaClient}
import gaia.io.JsonIO
import gaia.test.KafkaSuite
import org.scalatest._

import scala.collection.JavaConverters._
import scala.io.Source



class ClientSuite extends KafkaSuite {

  val MESSAGE_FILES = Array(
    "example/data/messages.1"
  )

  test("Sending Messages") {
    var config = new ConnectionConfig().Kafka(KAFKA_URL)
    val conn = new GaiaClient(config)
    val io = new JsonIO()
    MESSAGE_FILES.foreach( file => {
      Source.fromFile(file).getLines().foreach( x => {
        val y = io.ReadMap(x)
        conn.addMessage(y)
      })
    })
    
  }
}
