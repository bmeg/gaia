
package gaea.app

import gaea.lib.GraphConnection

object VertexCount {
  def main(args: Array[String]) {
    val conn = new GraphConnection(args(0))
    val graph = conn.connect()
    printf("Vertices: %s", graph.traversal().V().count() )
  }
}