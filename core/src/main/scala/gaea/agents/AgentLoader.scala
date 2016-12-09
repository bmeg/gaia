package gaea.agents

import gaea.api.{ActivationCriteria, Agent}

import scala.io.Source
import net.jcazevedo.moultingyaml._


class AgentDoc(act : ActivationCriteria) extends Agent {
  override def getActivation(): ActivationCriteria = {
    return act
  }
}

class VertexCreateReader


class AgentDocReader extends YamlReader[AgentDoc] {
  override def read(yaml: YamlValue): AgentDoc = {
    val act = yaml.asYamlObject.fields(YamlString("trigger")).convertTo[ActivationCriteria]
    var out = new AgentDoc(act)
    return out
  }
}

object AgentLoader {

  implicit class TriggerReader extends YamlReader[ActivationCriteria] {
    override def read(yaml: YamlValue): ActivationCriteria = {
      var out = new ActivationCriteria
      val o = yaml.asYamlObject.fields.get(YamlString("vertexCreate"))
      if (o.isDefined) {
        println( o.get.asYamlObject.fields.get(YamlString("type")) )
      }
      return out
    }
  }

  def LoadFile(path: String) : Agent = {
    val raw = Source.fromFile(path).getLines.mkString("\n")
    val doc = raw.parseYaml
    val agent = doc.convertTo[AgentDoc]
    return agent
  }


}