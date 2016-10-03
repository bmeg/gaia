package gaea.facet

import gaea.graph.GaeaGraph

import org.http4s._
import org.http4s.server._

trait GaeaFacet {
  def service(graph: GaeaGraph): HttpService
  def root: String
}
