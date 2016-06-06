lazy val core = project.in(file("core"))
lazy val server = project.in(file("server")).dependsOn(core)
lazy val file_io = project.in(file("file-io"))

