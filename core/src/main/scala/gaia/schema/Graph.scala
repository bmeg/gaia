package gaia.schema

case class Vertex(gid: String, label: String, properties: Map[String, Any] = Map())
case class Edge(in: String, label: String, out: String, properties: Map[String, Any] = Map())
case class Graph(vertexes: Map[String, Vertex] = Map(), in: Map[String, Seq[Edge]] = Map(), out: Map[String, Seq[Edge]] = Map()) {
  def incoming(point: String): Seq[Vertex] = {
    in(point).map(i => vertexes(i.out))
  }

  def outgoing(point: String): Seq[Vertex] = {
    out(point).map(o => vertexes(o.in))
  }
}

case class EdgeMaps(in: Map[String, List[Edge]], out: Map[String, List[Edge]])
object EdgeMaps {
  def empty: EdgeMaps = {
    new EdgeMaps(Map[String, List[Edge]](), Map[String, List[Edge]]())
  }
}

object Graph {
  def assemble(vertexes: List[Vertex], edges: List[Edge]): Graph = {
    val vertexMap = vertexes.foldLeft(Map[String, Vertex]()) { (m, vertex) =>
      m + (vertex.gid -> vertex)
    }

    val edgeMaps = edges.foldLeft(EdgeMaps.empty) { (edges, edge) =>
      val incoming: List[Edge] = edge :: edges.in.getOrElse(edge.out, List())
      val outgoing: List[Edge] = edge :: edges.out.getOrElse(edge.in, List())
      new EdgeMaps(edges.in + (edge.out -> incoming), edges.out + (edge.in -> outgoing))
    }

    Graph(vertexMap, edgeMaps.in, edgeMaps.out)
  }
}
