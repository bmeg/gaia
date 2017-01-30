package gaia.graph

import gaia.config._
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
    "typeIndex" -> Map("type" -> classOf[String]),
    "labelIndex" -> Map("#label" -> classOf[String]),
    "symbolIndex" -> Map("symbol" -> classOf[String])
  )

  def signature: String = "base-gaia-migration"

  def migrate(graph: GaiaGraph) {
    graph.makeIndexes(indexSpec)
  }
}

object GaiaMigrations {
  def findMigration(name: String): GaiaMigration = {
    val qualified = if (name.contains(".")) name else "gaia.graph.migration." + name
    val runtimeMirror = universe.runtimeMirror(getClass.getClassLoader)
    val module = runtimeMirror.staticModule(qualified)
    runtimeMirror.reflectModule(module).asInstanceOf[GaiaMigration]
  }

  def findMigrations(names: Seq[String]): List[GaiaMigration] = {
    names.map(findMigration).toList
  }

  def runMigrations(graph: GaiaGraph) (migrations: Seq[GaiaMigration]) = {
    for(migration <- migrations) {
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

  def migrate(config: GaiaConfig) = {
    val graph = config.connectToGraph(config.graph)
    if (graph.isSuccess) {
      val migrations = GaiaMigrations.findMigrations(config.graph.migrations.getOrElse(List[String]()))
      runMigrations(graph.get) (BaseMigration :: migrations)
    } else {
      println("failed to open graph! " + config.toString)
    }
  }
}
