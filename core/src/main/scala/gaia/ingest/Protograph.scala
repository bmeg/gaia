package gaia.ingest

/**
  * Created by ellrott on 12/18/16.
  */

import com.google.protobuf.util.JsonFormat
import gaia.schema.ProtoGraph
import gaia.schema.ProtoGraph.MessageConvert

class Protograph {

  def parse(message: String): Unit = {

    val b = MessageConvert.newBuilder()
    val parser = JsonFormat.parser()
    val v = new ProtoGraph()
    parser.merge(message, b)
    return b.build()
  }


}
