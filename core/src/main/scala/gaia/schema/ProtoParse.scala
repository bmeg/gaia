package gaia.schema

import java.io.FileInputStream
import com.google.protobuf.DescriptorProtos
import scala.collection.JavaConverters._

class Notation(input: String) {
  def getAction(): String = {
    if (input.startsWith("/EDGE_LINK")) {
      "EDGE_LINK"
    } else {
      "NONE"
    }
  }

  def getDest(): String = {
    input.split(":")(1)
  }
}

object ProtoParse {
  def main(args: Array[String]) = {
    val tmpfile = "/tmp/gaia.proto" //TODO: Fix this
    Runtime.getRuntime.exec(Array[String]("protoc", "-o", tmpfile, "--include_source_info", args(0) )).waitFor

    val b = new FileInputStream(tmpfile)
    val fileset = DescriptorProtos.FileDescriptorSet.newBuilder().mergeFrom(b).build()

    fileset.getFileList.asScala.foreach { protoFile =>
      val notations = protoFile.getSourceCodeInfo.getLocationList.asScala.filter { y =>
        y.getTrailingComments.length > 0 && y.getTrailingComments.startsWith("/")
      }.map { x =>
        if (x.getPath(0) == 4) { //is a message
          val messageType = protoFile.getMessageType(x.getPath(1))
          if (x.getPath(2) == 2) { //about a field
            (messageType, messageType.getField(x.getPath(2)), new Notation(x.getTrailingComments))
          } else {
            null
          }
        } else {
          null
        }
      }

      notations.foreach { x =>
        val note = "%s.%s %s %s".format( x._1.getName, x._2.getName, x._3.getAction(), x._3.getDest())
        println(note)
      }
    }
  }
}
