import sbt._

object GaeaBuild extends Build {
  lazy val server = RootProject(file("server"))
}

