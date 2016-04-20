package bmeg.io

import java.io.InputStream
import bmeg.io.Graph


class ProtoReader(inputStream: InputStream) extends Iterator[Graph.ElementRecord] {

  var next_val : Graph.ElementRecord = null

  def getNext(): Unit = {
    if (next_val == null) {
      try {
        next_val = Graph.ElementRecord.parseFrom(inputStream)
      } catch {
        case e : java.io.IOException => next_val = null
      }
    }
  }

  override def hasNext: Boolean = {
    getNext()
    return next_val != null
  }

  override def next(): Graph.ElementRecord = {
    getNext()
    var out = next_val
    next_val = null
    return out
  }

}