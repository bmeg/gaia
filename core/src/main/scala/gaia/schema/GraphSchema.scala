package gaia.schema

case class GaiaVertex(gid: String, label: String, properties: Map[String, Any])
case class GaiaEdge(in: String, label: String, out: String, properties: Map[String, Any])
case class GraphSchema(vertexes: Map[String, GaiaVertex], in: Map[String, Seq[GaiaEdge]], out: Map[String, Seq[GaiaEdge]]) {
  def incoming(point: String): Seq[GaiaVertex] = {
    in(point).map(i => vertexes(i.out))
  }

  def outgoing(point: String): Seq[GaiaVertex] = {
    out(point).map(o => vertexes(o.in))
  }
}

case class EdgeMaps(in: Map[String, List[GaiaEdge]], out: Map[String, List[GaiaEdge]])
object EdgeMaps {
  def empty: EdgeMaps = {
    new EdgeMaps(Map[String, List[GaiaEdge]](), Map[String, List[GaiaEdge]]())
  }
}

object GraphSchema {
  def assemble(vertexes: List[GaiaVertex], edges: List[GaiaEdge]): GraphSchema = {
    val vertexMap = vertexes.foldLeft(Map[String, GaiaVertex]()) { (m, vertex) =>
      m + (vertex.gid -> vertex)
    }

    val edgeMaps = edges.foldLeft(EdgeMaps.empty) { (edges, edge) =>
      val incoming: List[GaiaEdge] = edge :: edges.in.getOrElse(edge.out, List())
      val outgoing: List[GaiaEdge] = edge :: edges.out.getOrElse(edge.in, List())
      new EdgeMaps(edges.in + (edge.out -> incoming), edges.out + (edge.in -> outgoing))
    }

    GraphSchema(vertexMap, edgeMaps.in, edgeMaps.out)
  }
}
