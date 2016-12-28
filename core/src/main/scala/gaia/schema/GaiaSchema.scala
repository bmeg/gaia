package gaia.schema

import gaia.ingest.ProtoGrapher

case class GaiaSchema(types: GraphSchema, protograph: ProtoGrapher)

object GaiaSchema {
  def load(protopath: String): GaiaSchema = {
    val protograph = ProtoGrapher.load(protopath)
    val types = GraphSchema.assemble(List[GaiaVertex](), List[GaiaEdge]())
    new GaiaSchema(types, protograph)
  }
}
