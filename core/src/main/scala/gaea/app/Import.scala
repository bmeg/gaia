package gaea.app

import scala.io.Source
import gaea.client.{ConnectionConfig, GaeaClient}
import gaea.io.JsonIO


object Import {
  def main(args: Array[String]) = {
    var config = new ConnectionConfig().Kafka(args(0))
    val conn = new GaeaClient(config)

    val io = new JsonIO()

    Source.fromFile(args(1)).getLines().foreach( x => {
      val y = io.ReadMap(x)
      conn.addMessage(y)
    })

    conn.close()
  }

}
