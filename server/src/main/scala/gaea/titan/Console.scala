package gaea.titan

import gaea.eval.Eval
import gremlin.scala._

object Console {
  val graph = Titan.defaultGraph()

  def eval[T](query: String): T = {
    val context = "import gremlin.scala._; import gaea.titan.Console._; "
    Eval.eval[T](context + query)
  }

  def vertexesOfType(typeName: String): GremlinScala[Vertex, shapeless.HNil] = {
    Titan.typeQuery(graph) (typeName)
  }
}
