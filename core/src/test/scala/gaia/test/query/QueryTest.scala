package gaia.test.frame

import gaia.graph._
import gaia.query._
import gaia.test.TestGraph

import shapeless._
import gremlin.scala._
import org.scalatest._

class QueryTest extends FunSuite {
  val graph = TestGraph.read("example/data/variants.1")
  val sampleStep = StepLabel[Vertex]()

}
