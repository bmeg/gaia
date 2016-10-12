package gaea.query

import gaea.graph._

import gremlin.scala._
import shapeless._
import shapeless.ops.hlist.RightFolder

abstract class Operation {
  type GremlinVertex = GremlinScala[Vertex, HNil]
  type GremlinEdge = GremlinScala[Edge, HNil]

  type Input
  type Output

  def apply(input: Input): Output
}

case class VertexOperation(label: String) extends Operation {
  type Input = GaeaGraph
  type Output = GremlinVertex

  def apply(graph: Input): Output = {
    graph.graph.V.hasLabel(label)
  }
}

case class InOperation(edge: String) extends Operation {
  type Input = GremlinVertex
  type Output = GremlinVertex

  def apply(vertex: Input): Output = {
    vertex.in(edge)
  }
}

case class InEdgeOperation(edge: String) extends Operation {
  type Input = GremlinVertex
  type Output = GremlinEdge

  def apply(vertex: Input): Output = {
    vertex.inE(edge)
  }
}

case class InVertexOperation(note: String) extends Operation {
  type Input = GremlinEdge
  type Output = GremlinVertex

  def apply(edge: Input): Output = {
    edge.inV()
  }
}

trait ApplyOperationDefault extends Poly2 {
  implicit def default[T, L <: HList] = at[T, L]((_, acc) => acc)
}

object ApplyOperation extends ApplyOperationDefault {
  implicit def vertex[T, L <: HList] = at[VertexOperation, GaeaGraph]((t, acc) => t.apply(acc))
  implicit def in[T, L <: HList] = at[InOperation, GremlinScala[Vertex, HNil]]((t, acc) => t.apply(acc))
  implicit def inEdge[T, L <: HList] = at[InEdgeOperation, GremlinScala[Vertex, HNil]]((t, acc) => t.apply(acc))
  implicit def inVertex[T, L <: HList] = at[InVertexOperation, GremlinScala[Edge, HNil]]((t, acc) => t.apply(acc))
}

object Operation {
  def process[A <: HList](operations: A, graph: GaeaGraph) (implicit folder: RightFolder.Aux[A, GaeaGraph, ApplyOperation.type, GremlinScala[Vertex, HNil]]): GremlinScala[Vertex, HNil] = {
    operations.foldRight(graph) (ApplyOperation)
  }
}
