package gaia.facet

import gaia.graph.GaiaGraph

import org.http4s._
import org.http4s.server._

trait GaiaFacet {
  def service(graph: GaiaGraph): HttpService
  def root: String
}
