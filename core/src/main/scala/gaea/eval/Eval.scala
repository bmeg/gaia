package gaea.eval

import scala.reflect.runtime.universe._
import scala.tools.reflect.ToolBox
import javax.script.ScriptEngineManager
import java.io.StringWriter

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

  def eval[T](code: String): Either[String, T] = {
    val writer = new StringWriter()
    val context = engine.getContext
    context.setErrorWriter(writer)
    engine.setContext(context)

    try {
      Right(engine.eval(code).asInstanceOf[T])
    } catch {
      case e: javax.script.ScriptException => Left(writer.toString)
    }
  }
}
