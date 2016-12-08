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

}
