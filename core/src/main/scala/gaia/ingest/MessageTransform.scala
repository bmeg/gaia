package gaia.ingest


class TransformException(x:String) extends Exception(x) {}

trait MessageTransform {
  def ingestMessage(message: Map[String,Any])
}
