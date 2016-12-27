package gaia.app

import scala.io.Source
import gaia.client.{ConnectionConfig, GaiaClient}
import gaia.io.JsonIO

object Import {
  def main(args: Array[String]) = {
    var config = new ConnectionConfig().Kafka(args(0))
    val conn = new GaiaClient(config)

    Source.fromFile(args(1)).getLines().foreach( x => {
      val y = JsonIO.readMap(x)
      conn.addMessage(y)
    })

    conn.close()
  }
}
