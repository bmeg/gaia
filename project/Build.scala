import sbt._

object GaiaBuild extends Build {
  lazy val server = RootProject(file("server"))
}

