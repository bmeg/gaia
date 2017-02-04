lazy val core = project.in(file("core"))
lazy val server = project.in(file("server")).dependsOn(core)
