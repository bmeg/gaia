package gaea.test.frame

import gaea.graph._
import gaea.query._
import gaea.test.TestGraph

import shapeless._
import gremlin.scala._
import org.scalatest._

class QueryTest extends FunSuite {
  val graph = TestGraph.read("example/data/variants.1")
  val sampleStep = StepLabel[Vertex]()

  test("construction") {
    val operations = InOperation("sampleOf") :: VertexOperation("individual") :: HNil
    val result = Operation.process(operations, graph).toList
    assert(result.size == 4)
    assert(result.head.value[String]("type") == "Biosample")
  }

  test("has") {
    val operations = HasOperation("barcode", List("X-normal", "Y-tumor")) :: InOperation("sampleOf") :: VertexOperation("individual") :: HNil
    val result = Operation.process(operations, graph).toList
    assert(result.size == 2)
    assert(result.head.value[String]("type") == "Biosample")
  }

  // test("as") {
  //   val operations = AsOperation(sampleStep) :: HasOperation("barcode", List("X-normal", "Y-tumor")) :: InOperation("sampleOf") :: VertexOperation("individual") :: HNil
  //   val result = Operation.process(operations, graph).toList
  //   assert(result.size == 2)
  //   assert(result.head.value[String]("type") == "Biosample")
  // }
}
