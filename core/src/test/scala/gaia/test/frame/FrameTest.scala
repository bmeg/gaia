package gaia.test.frame

import gaia.graph._
import gaia.frame.Frame
import gaia.test.TestGraph

import org.scalatest._

class FrameTest extends FunSuite {
  val graph = TestGraph.read("example/data/expressions.1", "example/schema/expression.proto_graph")

  test("hydrating serialized vector") {
    val vertex = graph.typeQuery("geneExpression").head
    val expression = Frame.hydrate(vertex) ("expressions")
    assert(expression.keys.size > 0)
  }

  test("extracting dataframe out of expression vectors") {
    val vertexes = graph.typeQuery("geneExpression").limit(10).toList
    val frame = Frame.convertFrame("none") (vertexes) ("barcode") ("expressions")
    assert(frame.size == 11)
    assert(frame.map(_.size).toSet.size == 1)
  }
}
