package gaia.api


class ActivationCriteria {
  var addVertexType : String = null
}


trait Agent {

  def getActivation() : ActivationCriteria


}
