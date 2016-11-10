package gaea.test.events


import gaea.agents.AgentLoader
import org.scalatest.FunSuite


class EventSuite extends FunSuite {

  test("Load Single Agent Doc") {
    var hashAgent = AgentLoader.LoadFile("example/agents/id_hash/id_hash.agent.yml")
    var a = hashAgent.getActivation()
    assert(a.addVertexType == "GeneExpression")
  }


}
