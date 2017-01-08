package gaia.graph

import gremlin.scala._
import scala.reflect.runtime.universe

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

  def registerMigrations(migrations: Seq[GaiaMigration]) = {
    allMigrations ++= migrations
  }

  def findMigration(name: String): GaiaMigration = {
    val qualified = if (name.contains(".")) name else "gaia.graph.migration." + name
    val runtimeMirror = universe.runtimeMirror(getClass.getClassLoader)
    val module = runtimeMirror.staticModule(qualified)
    runtimeMirror.reflectModule(module).asInstanceOf[GaiaMigration]
  }

  def findMigrations(names: Seq[String]): Seq[GaiaMigration] = {
    names.map(findMigration)
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
