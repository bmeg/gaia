package gaia.schema

case class GaiaVertex(gid: String, label: String, properties: Map[String, Any])
case class GaiaEdge(in: String, label: String, out: String, properties: Map[String, Any])
case class GaiaSchema(vertexes: Map[String, GaiaVertex], in: Map[String, Seq[GaeaEdge]], out: Map[String, Seq[GaeaEdge]]) {
  def incoming(point: String): Seq[GaiaVertex] = {
    in(point).map(i => vertexes(i.out))
  }

  def outgoing(point: String): Seq[GaiaVertex] = {
    out(point).map(o => vertexes(o.in))
  }
}

object GaiaSchema {
  def assemble(vertexes: Seq[GaiaVertex], edges: Seq[GaiaEdge]): GaiaSchema = {
    
  }
}
