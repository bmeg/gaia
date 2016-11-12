package gaea.query

import gaea.graph._

import shapeless._
import shapeless.ops.hlist.RightFolder
import shapeless.ops.hlist.Prepend

import gremlin.scala._
import org.apache.tinkerpop.gremlin.process.traversal.P._

// case class VertexQuery(vertex: String) extends GaeaStatement
// case class InQuery(in: String) extends GaeaStatement
// case class OutQuery(out: String) extends GaeaStatement
// case class InEdgeQuery(edge: String) extends GaeaStatement
// case class OutEdgeQuery(edge: String) extends GaeaStatement
// case class InVertexQuery(vertex: String) extends GaeaStatement
// case class OutVertexQuery(vertex: String) extends GaeaStatement
// case class WithinQuery(property: String, within: List[String])
// case class HasQuery(has: List[WithinQuery]) extends GaeaStatement

// case class SelectQuery(steps: List[String]) extends GaeaStatement
// case class AsQuery(as: String) extends GaeaStatement

abstract class Operation {
  // type GremlinVertex = GremlinScala[Vertex, HNil]
  // type GremlinEdge = GremlinScala[Edge, HNil]

  // val gids = collection.mutable.Map[String, Key[String]]()

  // def findGid(key: String): Key[String] = {
  //   gids.get(key).getOrElse {
  //     val gid = Key[String](key)
  //     gids(key) = gid
  //     gid
  //   }
  // }
}

case class VertexOperation[Labels <: HList](vertex: String) extends Operation {
  def operate(graph: GaeaGraph): GremlinScala[Vertex, Labels] = {
    graph.typeQuery(vertex).asInstanceOf[GremlinScala[Vertex, Labels]]
  }
}

case class HasOperation[M, Labels <: HList](key: String, values: List[M]) extends Operation {
  def operate(vertex: GremlinScala[Vertex, Labels]): GremlinScala[Vertex, Labels] = {
    val gid = Key[M](key)
    vertex.has(gid, within(values:_*))
  }
}

case class AsOperation[A, In <: HList, Out <: HList](step: String) extends Operation {
  // def operate(g: GremlinScala[A, In]): GremlinScala[A, B :: In] = {
  //   g.as(step)
  // }

  def operate(g: GremlinScala[A, In]) (implicit p: Prepend[In, ::[A, HNil]]): GremlinScala[A, p.Out] = {
    g.as(step)
  }
}

case class InOperation[Labels <: HList](in: String) extends Operation {
  def operate(vertex: GremlinScala[Vertex, Labels]): GremlinScala[Vertex, Labels] = {
    vertex.in(in)
  }
}

case class OutOperation[Labels <: HList](out: String) extends Operation {
  def operate(vertex: GremlinScala[Vertex, Labels]): GremlinScala[Vertex, Labels] = {
    vertex.out(out)
  }
}

case class InEdgeOperation[Labels <: HList](edge: String) extends Operation {
  def operate(vertex: GremlinScala[Vertex, Labels]): GremlinScala[Edge, Labels] = {
    vertex.inE(edge)
  }
}

case class OutEdgeOperation[Labels <: HList](edge: String) extends Operation {
  def operate(vertex: GremlinScala[Vertex, Labels]): GremlinScala[Edge, Labels] = {
    vertex.outE(edge)
  }
}

case class InVertexOperation[Labels <: HList](note: String) extends Operation {
  def operate(edge: GremlinScala[Edge, Labels]): GremlinScala[Vertex, Labels] = {
    edge.inV()
  }
}

case class OutVertexOperation[Labels <: HList](note: String) extends Operation {
  def operate(edge: GremlinScala[Edge, Labels]): GremlinScala[Vertex, Labels] = {
    edge.outV()
  }
}

trait ApplyOperationDefault extends Poly2 {
  implicit def default[T, L <: HList] = at[T, L] ((_, acc) => acc)
}

object ApplyOperation extends ApplyOperationDefault {
  implicit def vertex[T, L <: HList, S <: HList] = at[VertexOperation[S], GaeaGraph] ((t, acc) => t.operate(acc))
  implicit def has[M, T, L <: HList, S <: HList] = at[HasOperation[M, S], GremlinScala[Vertex, S]] ((t, acc) => t.operate(acc))
  implicit def as[A, T, L <: HList, In <: HList, Out <: HList] = at[AsOperation[A, In, Out], GremlinScala[A, In]] ((t, acc) => t.operate(acc))
  implicit def in[T, L <: HList, S <: HList] = at[InOperation[S], GremlinScala[Vertex, S]] ((t, acc) => t.operate(acc))
  implicit def out[T, L <: HList, S <: HList] = at[OutOperation[S], GremlinScala[Vertex, S]] ((t, acc) => t.operate(acc))
  implicit def inEdge[T, L <: HList, S <: HList] = at[InEdgeOperation[S], GremlinScala[Vertex, S]] ((t, acc) => t.operate(acc))
  implicit def outEdge[T, L <: HList, S <: HList] = at[OutEdgeOperation[S], GremlinScala[Vertex, S]] ((t, acc) => t.operate(acc))
  implicit def inVertex[T, L <: HList, S <: HList] = at[InVertexOperation[S], GremlinScala[Edge, S]] ((t, acc) => t.operate(acc))
  implicit def outVertex[T, L <: HList, S <: HList] = at[OutVertexOperation[S], GremlinScala[Edge, S]] ((t, acc) => t.operate(acc))
}

object Operation {
  def process[Input, Output, A <: HList](operations: A, input: Input) (implicit folder: RightFolder.Aux[A, Input, ApplyOperation.type, Output]): Output = {
    operations.foldRight(input) (ApplyOperation)
  }
}
