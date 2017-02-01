package gaia.schema

import gaia.protograph.{Protograph => P}

case class GaiaSchema(types: GraphSchema, protograph: P)

object GaiaSchema {
  def load(protopath: String): GaiaSchema = {
    val protograph = P.loadProtograph(protopath)
    val types = GraphSchema.assemble(List[GaiaVertex](), List[GaiaEdge]())
    new GaiaSchema(types, protograph)
  }
}
