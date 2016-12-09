package gaia.schema

case class GaiaVertex(label: String, properties: Map[String, Any], in: List[String], out: List[String])
case class GaiaEdge(label: String, properties: Map[String, Any], in: String, out: String)

case class GaiaSchema(vertexes: Map[String, GaiaVertex], edges: Map[String, GaiaEdge])
