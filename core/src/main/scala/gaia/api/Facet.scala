package gaia.api

import com.google.protobuf.Message

trait Method {
  def apply(message: Message) : Message
}


trait Facet {
  def getRoutes() : List[String]
  def getMethod(route: String) : Method
}
