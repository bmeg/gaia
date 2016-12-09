package gaia.titan

import gremlin.scala._
import argonaut._, Argonaut._, ArgonautShapeless._

import shapeless._
import StringWrap._
import union._
import syntax.singleton._

object Query {
  type QueryElement = Union.`'VertexQuery -> VertexQuery, 'HasQuery -> HasQuery, 'WithinQuery -> WithinQuery, 'AsQuery -> AsQuery, 'InQuery -> InQuery, 'OutQuery -> OutQuery, 'InEQuery -> InEQuery, 'OutEQuery -> OutEQuery, 'InVQuery -> InVQuery, 'OutVQuery -> OutVQuery, 'SelectQuery -> SelectQuery`.T

  final case class VertexQuery(vertex: String)
  final case class HasQuery(has: Map[String, String])
  final case class WithinQuery(has: Map[String, List[String]])
  final case class AsQuery(as: String)
  final case class InQuery(in: String)
  final case class OutQuery(out: String)
  final case class InEQuery(inE: String)
  final case class OutEQuery(outE: String)
  final case class InVQuery(inV: String)
  final case class OutVQuery(outV: String)
  final case class SelectQuery(select: List[String])

  val encodeVertexQuery = EncodeJson.of[VertexQuery]
  val encodeHasQuery = EncodeJson.of[HasQuery]
  val encodeWithinQuery = EncodeJson.of[WithinQuery]
  val encodeAsQuery = EncodeJson.of[AsQuery]
  val encodeInQuery = EncodeJson.of[InQuery]
  val encodeOutQuery = EncodeJson.of[OutQuery]
  val encodeInEQuery = EncodeJson.of[InEQuery]
  val encodeOutEQuery = EncodeJson.of[OutEQuery]
  val encodeInVQuery = EncodeJson.of[InVQuery]
  val encodeOutVQuery = EncodeJson.of[OutVQuery]
  val encodeSelectQuery = EncodeJson.of[SelectQuery]
  val encodeQueryElement = EncodeJson.of[QueryElement]

  // def decode(json: String): QueryElement = {
  //   json.decodeOption[QueryElement]
  // }

  trait GaiaQuery {
    def operate(step: GremlinScala[Vertex, shapeless.HNil]): GremlinScala[Vertex, shapeless.HNil]
  }
}
