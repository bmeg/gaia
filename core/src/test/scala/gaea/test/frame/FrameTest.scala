package gaea.test.frame

import org.scalatest._

import gaea.titan.Titan
import gaea.frame.Frame

class FrameTest extends FunSuite {
  test("hydrating serialized vector") {
    val vertex = Titan.typeQuery(Titan.connection) ("geneExpression").head
    val expression = Frame.hydrate(vertex) ("expressions")
    assert(expression.keys.size > 0)
  }

  test("extracting dataframe out of expression vectors") {
    val vertexes = Titan.typeQuery(Titan.connection) ("geneExpression").limit(10).toList
    val frame = Frame.convertFrame("none") (vertexes) ("barcode") ("expressions")
    assert(frame.size == 11)
    assert(frame.map(_.size).toSet.size == 1)
  }
}
