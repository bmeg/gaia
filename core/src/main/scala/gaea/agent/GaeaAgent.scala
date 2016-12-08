package gaea.agent

import gaea.message._
import gaea.query._

trait GaeaAgent {
  def trigger(message: GaeaMessage): Boolean
  def query: GaeaQuery
}
