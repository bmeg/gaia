package gaea.titan

import gaea.eval.Eval

object Console {
  val graph = Titan.defaultGraph()

  def eval[T](query: String): T = {
    val context = "import gremlin.scala._; import gaea.titan.Console._; "
    Eval.eval[T](context + query)
  }
}
