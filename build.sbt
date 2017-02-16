scalaVersion in ThisBuild := "2.11.8"
lazy val core = project.in(file("core"))
lazy val server = project.in(file("server")).dependsOn(core)
