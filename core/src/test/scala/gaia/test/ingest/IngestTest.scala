
package gaia.test.ingest

import gaia.api.Ingestor
import gaia.api.ingest.FileIngestor
import org.scalatest.FunSuite


class IngestSuite extends FunSuite {


  test("Test File Based Ingestor") {
    val in : Ingestor = new FileIngestor("example/data/messages.1")

    var messageCount = 0
    in.setMessageCallback( (x) => {
      println(x)
      messageCount += 1
    } )

    var running = true
    in.setCloseCallback( (x) => {
      running = false
    } )

    in.start()
    var loopCount = 0
    while (running && loopCount < 10) {
      Thread.sleep(1000)
      loopCount += 1
    }
    assert(messageCount == 2)
  }

}
