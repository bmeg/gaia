package gaia.graph

import gremlin.scala._

trait GaiaMigration {
  def signature: String
  def migrate(graph: GaiaGraph)
}

object BaseMigration extends GaiaMigration {
  val indexSpec = Map(
    "idIndex" -> Map("id" -> classOf[String]),
    "gidIndex" -> Map("gid" -> classOf[String]),
    "typeIndex" -> Map("type" -> classOf[String])
  )

  def signature: String = "base-gaia-migration"

  def migrate(graph: GaiaGraph) {
    graph.makeIndexes(indexSpec)
  }
}

object GaiaMigrations {
  val allMigrations = scala.collection.mutable.ListBuffer.empty[GaiaMigration]
  allMigrations += BaseMigration

  def registerMigrations(migrations: List[GaiaMigration]) = {
    allMigrations ++= migrations
  }

  def runMigrations(graph: GaiaGraph) = {
    for(migration <- allMigrations) {
      val key = "migration:" + migration.signature
      if (graph.V.has(Gid, key).toList.isEmpty) {
        println("running migration " + key)
        migration.migrate(graph)

        val vertex = graph.graph + ("migration", Gid -> key)
        graph.associateType(vertex) ("migration")
        graph.commit()
      }
    }
  }
}
