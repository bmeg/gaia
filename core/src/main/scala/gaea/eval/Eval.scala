package gaea.eval

import scala.reflect.runtime.universe._
import scala.tools.reflect.ToolBox
import javax.script.ScriptEngineManager

object Eval {
  val toolbox = runtimeMirror(getClass.getClassLoader).mkToolBox()

  def eval[T](code: String): T = {
    toolbox.eval(toolbox.parse(code)).asInstanceOf[T]
  }
}

class ReplContainer

object Repl {
  val engine = new ScriptEngineManager().getEngineByName("scala")
  val settings = engine.asInstanceOf[scala.tools.nsc.interpreter.IMain].settings
  settings.embeddedDefaults[ReplContainer]

  def eval[T](code: String): T = {
    engine.eval(code).asInstanceOf[T]
  }
}
