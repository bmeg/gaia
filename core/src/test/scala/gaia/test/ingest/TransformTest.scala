package gaia.test.ingest

import gaia.api.Ingestor
import gaia.api.ingest.FileIngestor
import gaia.config.GaiaConfig
import gaia.ingest.{GraphTransform, ProtoGrapher}
import org.scalatest.FunSuite

/**
  * Created by ellrott on 12/22/16.
  */
class TransformTest extends FunSuite {


  test("Test File Based Ingestor and Transformation") {
    val in : Ingestor = new FileIngestor("example/data/social.1")

    val graph = GaiaConfig.memoryGraph()
    val protographer = ProtoGrapher.load("example/schema/social.proto_graph")
    val trans = new GraphTransform(graph, protographer)

    println(protographer.msgs)
    var messageCount = 0
    in.setMessageCallback( (x) => {
      trans.ingestMessage(x)
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
