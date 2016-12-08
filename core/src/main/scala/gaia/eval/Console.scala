package gaia.eval

import gaia.graph._
import gremlin.scala._

class Console(graph: GaiaGraph) {
  initializeRepl()

  def eval[T](query: String): T = {
    val context = "import gremlin.scala._; import gaia.eval.Console._; "
    Eval.eval[T](context + query)
  }

  def initializeRepl(): Boolean = {
    Repl.eval[Unit]("import gremlin.scala._; import gaia.titan.Titan._; val graph = defaultGraph()")
    true
  }

  def interpret[T](code: String): Either[String, T] = {
    Repl.eval[T](code)
  }

  def vertexesOfType(typeName: String): GremlinScala[Vertex, shapeless.HNil] = {
    graph.typeQuery(typeName)
  }
}
