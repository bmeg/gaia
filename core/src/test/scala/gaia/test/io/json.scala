
package gaia.test.io

import org.scalatest._
import gaia.io.JsonIO
import scala.collection.JavaConverters._


class IOSuite extends FunSuite {
  test("Parse JSON") {
    var input = """{ "test" : 1 }"""
    val o = JsonIO.readMap(input)
    assert( o.get("test") == 1 )
  }

  test("Write JSON") {
    val i = new scala.collection.mutable.HashMap[String,Object]()
    i.put("test", Int.box(1))
    val a = JsonIO.writeMap(i.toMap)
    assert( a == """{"test":1}""" )
  }

}
