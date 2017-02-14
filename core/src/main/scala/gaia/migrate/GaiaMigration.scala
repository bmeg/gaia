package gaia.migrate

import gaia.config._
import gaia.graph._
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

case class MigrationMirror(symbol: String) extends GaiaMigration {
  val qualified = if (symbol.contains(".")) symbol else "gaia.migrate." + symbol
  val mirror = universe.runtimeMirror(getClass.getClassLoader)
  val module = mirror.staticModule(qualified)
  val space = mirror.reflectModule(module)
  val instance = mirror.reflect(space.instance)

  val signatureSymbol = module.typeSignature.declarations.filter(_.name.toString() == "signature").head.asMethod
  val signatureMethod = instance.reflectMethod(signatureSymbol)
  val migrateSymbol = module.typeSignature.declarations.filter(_.name.toString() == "migrate").head.asMethod
  val migrateMethod = instance.reflectMethod(migrateSymbol)

  def signature: String = {
    signatureMethod.apply().asInstanceOf[String]
  }

  def migrate(graph: GaiaGraph) {
    migrateMethod.apply(graph)
  }
}

object GaiaMigrations {
  def findMigrations(names: Seq[String]): List[GaiaMigration] = {
    names.map(name => MigrationMirror(name)).toList // findMigration).toList
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
