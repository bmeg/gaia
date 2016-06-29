package gaea.titan

import gaea.eval.Eval
import gaea.eval.Repl
import gremlin.scala._

object Console {
  val graph = Titan.defaultGraph()
  initializeRepl()

  def eval[T](query: String): T = {
    val context = "import gremlin.scala._; import gaea.titan.Console._; "
    Eval.eval[T](context + query)
  }

  def initializeRepl(): Boolean = {
    Repl.eval[Unit]("import gremlin.scala._; import gaea.titan.Titan._; val graph = defaultGraph()")
    true
  }

  def interpret[T](code: String): Either[String, T] = {
    Repl.eval[T](code)
  }

  def vertexesOfType(typeName: String): GremlinScala[Vertex, shapeless.HNil] = {
    Titan.typeQuery(graph) (typeName)
  }
}
