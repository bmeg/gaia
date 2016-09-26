
package gaea.test.io

import org.scalatest._
import gaea.io.JsonIO
import scala.collection.JavaConverters._


class IOSuite extends FunSuite {
  test("Parse JSON") {
    var input = """{ "test" : 1 }"""
    val io = new JsonIO()
    val o = io.ReadMap(input)
    assert( o.get("test") == 1 )
  }

  test("Write JSON") {
    val io = new JsonIO()
    val i = new scala.collection.mutable.HashMap[Object,Object]()
    i.put("test", Int.box(1))
    val a = io.WriteMap(i.asJava)
    assert( a == """{"test":1}""" )
  }

}