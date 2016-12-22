package gaia.ingest

import scala.io.Source

class TransformException(x:String) extends Exception(x) {

}

trait MessageTransform {
  def ingestMessage(message: java.util.Map[String,Object])

}
