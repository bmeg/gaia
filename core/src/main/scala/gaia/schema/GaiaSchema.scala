package gaia.schema

import gaia.ingest.Protographer

case class GaiaSchema(types: GraphSchema, protograph: Protographer)

object GaiaSchema {
  def load(protopath: String): GaiaSchema = {
    val protograph = Protographer.load(protopath)
    val types = GraphSchema.assemble(List[GaiaVertex](), List[GaiaEdge]())
    new GaiaSchema(types, protograph)
  }
}
