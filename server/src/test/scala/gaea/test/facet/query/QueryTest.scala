package gaea.test.query

import gaea.graph._
import gaea.query._
import gaea.facet.QueryFacet
import gaea.test.TestGraph

import shapeless._
import gremlin.scala._
import org.json4s._
import org.json4s.jackson.JsonMethods._

import org.scalatest._

class QueryTest extends FunSuite {
  val graph = TestGraph.read("example/data/variants.1")

 //  val query = """[{"vertex": "Gene"},
 // {"has": {"symbol": ["AHI3", "HOIK4L"]}},
 // {"in": "inGene"},
 // {"out": "effectOf"},
 // {"out": "tumorSample"},
 // {"in": "expressionFor"},
 // {"as": "expressionStep"},
 // {"inE": "appliesTo"},
 // {"as": "levelStep"},
 // {"outV": ""},
 // {"as": "signatureStep"},
 // {"select": ["signatureStep", "levelStep", "expressionStep"]}]"""

  test("operation") {
    val raw = """{"query": [{"vertex": "gene"},{"in": "inGene"}]}"""
    val json = parse(raw)
    val query = QueryFacet.toQuery(json)
    val result = query.operate[GremlinScala[Vertex, HList]](graph)
    assert(result.toList.size == 7)
  }
}
