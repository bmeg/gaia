package gaea.facet

import gaea.graph._
// import gaea.query._
import leprechaun._

import org.json4s._
import org.json4s.jackson._
import org.json4s.jackson.JsonMethods._

object QueryFacet {
 //  class OperationSerializer extends CustomSerializer[Operation](format => ({
 //    case JObject(List(JField("vertex", JString(vertex)))) => VertexOperation(vertex)
 //    case JObject(List(JField("in", JString(in)))) => InOperation(in)
 //  }, {
 //    case VertexOperation(vertex) => JObject(JField("vertex", JString(vertex)))
 //    case InOperation(in) => JObject(JField("in", JString(in)))
 //  }))

 //  implicit val formats = Serialization.formats(NoTypeHints) + new OperationSerializer()

 //  def toQuery(json: JValue): GaeaQuery = {
 //    json.extract[GaeaQuery]
 //  }

 //  val example = """{"query": [{"vertex": "gene"}, {"in": "inGene"}]}"""

 // //  val example = """[{"vertex": "Gene"},
 // // {"has": "symbol", "within": ["AHI3", "HOIK4L"]},
 // // {"in": "inGene"},
 // // {"out": "effectOf"},
 // // {"out": "tumorSample"},
 // // {"in": "expressionFor"},
 // // {"as": "expressionStep"},
 // // {"inE": "appliesTo"},
 // // {"as": "levelStep"},
 // // {"outV": ""},
 // // {"as": "signatureStep"},
 // // {"select": ["signatureStep", "levelStep", "expressionStep"]}]"""

 // // {"has": {"symbol": ["AHI3", "HOIK4L"]}},
}
