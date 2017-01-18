lazy val core = project.in(file("core"))
//lazy val kafka = project.in(file("kafka")).dependsOn(core)
lazy val server = project.in(file("server")).dependsOn(core)
lazy val command = project.in(file("command")).dependsOn(server)
lazy val file_io = project.in(file("file-io"))

