package gaia.schema

import gaia.protograph.{Protograph => P}

case class GaiaSchema(types: Graph, protograph: P)

object GaiaSchema {
  def load(protopath: String): GaiaSchema = {
    val protograph = P.loadProtograph(protopath)
    val types = Graph.assemble(List[Vertex](), List[Edge]())
    new GaiaSchema(types, protograph)
  }
}
