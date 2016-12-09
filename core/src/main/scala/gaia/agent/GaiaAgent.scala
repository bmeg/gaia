package gaia.agent

import gaia.message._
import gaia.query._

trait GaiaAgent {
  def trigger(message: GaiaMessage): Boolean
  def query: GaiaQuery
}
