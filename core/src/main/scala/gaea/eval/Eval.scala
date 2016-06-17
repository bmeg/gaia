package gaea.eval

import scala.reflect.runtime.universe._
import scala.tools.reflect.ToolBox

object Eval {
  val toolbox = runtimeMirror(getClass.getClassLoader).mkToolBox()

  def eval[T](code: String): T = {
    toolbox.eval(toolbox.parse(code)).asInstanceOf[T]
  }
}
