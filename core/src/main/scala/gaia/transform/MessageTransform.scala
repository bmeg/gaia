package gaia.transform

class TransformException(x:String) extends Exception(x) {}

trait MessageTransform {
  def transform(message: Map[String,Any])
}
