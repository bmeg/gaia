package gaia.test.ingest

import gaia.api.Ingestor
import gaia.api.ingest.FileIngestor
import gaia.config.GaiaConfig
import gaia.ingest.{GraphTransform, ProtoGrapher}
import org.scalatest.FunSuite
import scala.collection.JavaConverters._


/**
  * Created by ellrott on 12/22/16.
  */
class TransformTest extends FunSuite {


  test("Test File Based Ingestor and Transformation") {
    val in : Ingestor = new FileIngestor("example/data/social.1")

    val gaia = GaiaConfig.memoryGraph()
    val protographer = ProtoGrapher.load("example/schema/social.proto_graph")
    val trans = new GraphTransform(gaia, protographer)

    //println(protographer.msgs)
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

    val g = gaia.graph()


    g.vertices().asScala.foreach( x => {
      println(x.id(), x.properties().asScala.mkString(","))
    })
    g.edges().asScala.foreach( x => {
      println(x)
    })

    assert( g.traversal().V().has("firstName", "Alex").tryNext().get().property("lastName").value().asInstanceOf[String] == "Adams" )

    println(g.traversal().V().outE().toList )
    //TODO: put assert statements here


  }
}
