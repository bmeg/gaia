
package gaea.test.io

import org.scalatest._
import gaea.io.JsonIO


class IOSuite extends FunSuite {
  test("Parse JSON") {
    var input = """{ "test" : 1 }"""
    val io = new JsonIO()
    val o = io.ReadMap(input)
    assert( o.get("test") == 1 )
  }
}