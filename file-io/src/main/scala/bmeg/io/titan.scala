
package bmeg.io

import bmeg.io.Graph.ElementRecord
import com.thinkaurelius.titan.core.TitanGraph
import org.apache.tinkerpop.gremlin.structure.{Vertex,Edge,Element}
import scala.collection.JavaConverters._

class TitanReader(graph: TitanGraph) extends Iterator[Graph.ElementRecord] {

  var mode : Int = 0
  var vertexIter : java.util.Iterator[Vertex] = null
  var edgeIter : java.util.Iterator[Edge] = null
  var nextElement : Graph.ElementRecord = null

  def get_next() = {
    if (nextElement == null) {
      if (mode == 0) {
        if (vertexIter == null) {
          vertexIter = graph.vertices()
        }
        if (!vertexIter.hasNext()) {
          mode = 1
        } else {
          nextElement = vertexConvert(vertexIter.next())
        }
      }
      if (mode == 1) {
        if (edgeIter == null) {
          edgeIter = graph.edges()
        }
        if (!edgeIter.hasNext) {
          mode = 2
        } else {
          nextElement = edgeConvert(edgeIter.next())
        }
      }
    }
  }

  def elementConvert(ein: Element) : Graph.ElementBase = {
    var b = Graph.ElementBase.newBuilder()
    b.setId( ein.id().asInstanceOf[Long] )
    ein.properties().asScala.foreach( x => {
      var p = Graph.Property.newBuilder()
      p.setKey(x.key())
      val v = x.value()
      if (v.getClass == classOf[String]) {
        p.setStringValue(v.asInstanceOf[String])
      }
      b.addProps(p)
    })
    return b.build()
  }

  def vertexConvert(vin: Vertex) : Graph.ElementRecord = {
    var v = Graph.VertexRecord.newBuilder()
    v.setBase(elementConvert(vin))
    return Graph.ElementRecord.newBuilder().setVertex(v).build()
  }

  def edgeConvert(ein: Edge) : Graph.ElementRecord = {
    var e = Graph.EdgeRecord.newBuilder()
    e.setBase(elementConvert(ein))
    return Graph.ElementRecord.newBuilder().setEdge(e).build()

  }

  override def hasNext: Boolean = {
    get_next()
    return nextElement == null
  }

  override def next(): ElementRecord = {
    val out = nextElement
    nextElement = null
    get_next()
    return out
  }

}