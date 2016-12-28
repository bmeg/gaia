package gaia.test.ingest

import gaia.api.Ingestor
import gaia.api.ingest.FileIngestor
import gaia.config.GaiaConfig
import gaia.schema.GaiaSchema
import gaia.graph.GaiaGraph
import gaia.ingest.{GraphTransform, Protographer}
import org.scalatest.FunSuite
import scala.collection.JavaConverters._

/**
  * Created by ellrott on 12/22/16.
  */

class TransformTest extends FunSuite {
  test("Test File Based Ingestor and Transformation") {
    val in: Ingestor = new FileIngestor("example/data/social.1")
    val gaia: GaiaGraph = GaiaConfig.memoryGraph("example/schema/social.protograph")

    // val protographer = Protographer.load("example/schema/social.proto_graph")

    // case class TestSchema(protograph: Protographer) extends GaiaSchema
    // val schema = new TestSchema(protographer)

    val transform = new GraphTransform(gaia)

    //println(protographer.msgs)
    var messageCount = 0
    in.setMessageCallback( (x) => {
      transform.transform(x)
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

    //Debugging print statements, will delete these someday
    println("VERTEXES ----------------")
    g.vertices().asScala.foreach{ x =>
      println(x.id(), x.properties().asScala.mkString(","))
    }

    println("EDGES ----------------")
    g.edges().asScala.foreach{ x =>
      println(x)
    }

    println("FRIENDS ----------------")
    g.traversal().V().has("firstName", "Alex").out("hasFriend").toList.asScala.foreach{ x =>
      println(x.properties().asScala.mkString(","))
    }

    println(g.traversal().V().has("firstName", "Alex").out("hasFriend").has("firstName", "Cynthia").toList)

    println("PETS ----------------")
    println(g.traversal().V().has("firstName", "Cynthia").out("hasPet").properties().toList)

    assert(g.traversal().V().has("firstName", "Alex").tryNext().get().property("lastName").value().asInstanceOf[String] == "Adams")
    assert(g.traversal().V().has("firstName", "Alex").out("hasFriend").has("firstName", "Cynthia").toList.size() == 1)
    assert(g.traversal().V().has("firstName", "Cynthia").out("hasPet").has("name", "Killer").toList.size() == 1)
  }
}
