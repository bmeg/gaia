package gaea.query

import gaea.graph._

import gremlin.scala._
import shapeless._
import shapeless.ops.hlist.RightFolder

// sealed abstract class GaeaStatement

// case class VertexQuery(vertex: String) extends GaeaStatement
// case class AsQuery(as: String) extends GaeaStatement
// case class InQuery(in: String) extends GaeaStatement
// case class OutQuery(out: String) extends GaeaStatement
// case class InEdgeQuery(edge: String) extends GaeaStatement
// case class OutEdgeQuery(edge: String) extends GaeaStatement
// case class InVertexQuery(vertex: String) extends GaeaStatement
// case class OutVertexQuery(vertex: String) extends GaeaStatement
// case class SelectQuery(steps: List[String]) extends GaeaStatement

// case class WithinQuery(property: String, within: List[String])
// case class HasQuery(has: List[WithinQuery]) extends GaeaStatement

// case class GaeaQuery(statements: List[GaeaStatement]) {
//   def execute(graph: GaeaGraph) {
    
//   }
// }

abstract class Operation {
  type GremlinVertex = GremlinScala[Vertex, HNil]
  type GremlinEdge = GremlinScala[Edge, HNil]
}

case class VertexOperation(vertex: String) extends Operation {
  def operate(graph: GaeaGraph): GremlinVertex = {
    graph.typeQuery(vertex)
  }
}

case class InOperation(in: String) extends Operation {
  def operate(vertex: GremlinVertex): GremlinVertex = {
    vertex.in(in)
  }
}

case class OutOperation(out: String) extends Operation {
  def operate(vertex: GremlinVertex): GremlinVertex = {
    vertex.out(out)
  }
}

case class InEdgeOperation(edge: String) extends Operation {
  def operate(vertex: GremlinVertex): GremlinEdge = {
    vertex.inE(edge)
  }
}

case class OutEdgeOperation(edge: String) extends Operation {
  def operate(vertex: GremlinVertex): GremlinEdge = {
    vertex.outE(edge)
  }
}

case class InVertexOperation(note: String) extends Operation {
  def operate(edge: GremlinEdge): GremlinVertex = {
    edge.inV()
  }
}

case class OutVertexOperation(note: String) extends Operation {
  def operate(edge: GremlinEdge): GremlinVertex = {
    edge.outV()
  }
}

trait ApplyOperationDefault extends Poly2 {
  implicit def default[T, L <: HList] = at[T, L] ((_, acc) => acc)
}

object ApplyOperation extends ApplyOperationDefault {
  implicit def vertex[T, L <: HList] = at[VertexOperation, GaeaGraph] ((t, acc) => t.operate(acc))
  implicit def in[T, L <: HList] = at[InOperation, GremlinScala[Vertex, HNil]] ((t, acc) => t.operate(acc))
  implicit def out[T, L <: HList] = at[OutOperation, GremlinScala[Vertex, HNil]] ((t, acc) => t.operate(acc))
  implicit def inEdge[T, L <: HList] = at[InEdgeOperation, GremlinScala[Vertex, HNil]] ((t, acc) => t.operate(acc))
  implicit def outEdge[T, L <: HList] = at[OutEdgeOperation, GremlinScala[Vertex, HNil]] ((t, acc) => t.operate(acc))
  implicit def inVertex[T, L <: HList] = at[InVertexOperation, GremlinScala[Edge, HNil]] ((t, acc) => t.operate(acc))
  implicit def outVertex[T, L <: HList] = at[OutVertexOperation, GremlinScala[Edge, HNil]] ((t, acc) => t.operate(acc))
}

object Operation {
  def process[Input, Output, A <: HList](operations: A, input: Input) (implicit folder: RightFolder.Aux[A, Input, ApplyOperation.type, Output]): Output = {
    operations.foldRight(input) (ApplyOperation)
  }
}
