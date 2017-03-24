organization  := "io.bmeg"
name := "gaia-core"

version := "0.0.7-SNAPSHOT"

scalaVersion := "2.11.8"

conflictManager := ConflictManager.strict.copy(organization = "com.esotericsoftware.*")

libraryDependencies ++= Seq(
  "io.bmeg"                      %% "ophion"                   % "0.0.7-SNAPSHOT",

  "com.fasterxml.jackson.module" %% "jackson-module-scala"     % "2.8.4",
  "com.michaelpollmeier"         %% "gremlin-scala"            % "3.1.2-incubating.0",
  "net.jcazevedo"                %% "moultingyaml"             % "0.3.0",
  "com.github.scopt"             %% "scopt"                    % "3.5.0",
  "com.trueaccord.scalapb"       %% "scalapb-json4s"           % "0.1.6",
  "org.scalactic"                %% "scalactic"                % "3.0.0",
  "org.scalatest"                %% "scalatest"                % "3.0.0" % "test",

  "org.scala-lang"               %  "scala-compiler"           % "2.11.8",
  // "org.apache.tinkerpop"         %  "gremlin-core"             % "3.1.1-incubating",
  "com.google.protobuf"          %  "protobuf-java"            % "3.1.0",
  "com.google.protobuf"          %  "protobuf-java-util"       % "3.1.0",
  "com.google.protobuf"          %  "protoc"                   % "3.1.0",
  "org.janusgraph"               %  "janusgraph"               % "0.1.0-SNAPSHOT"
  // "com.thinkaurelius.titan"      %  "titan-core"               % "1.1.0-SNAPSHOT",
  // "com.thinkaurelius.titan"      %  "titan-cassandra"          % "1.1.0-SNAPSHOT",
  // "com.thinkaurelius.titan"      %  "titan-es"                 % "1.1.0-SNAPSHOT"
  // "org.json4s"                   %% "json4s-native"            % "3.3.0",
  // "org.json4s"                   %% "json4s-jackson"           % "3.3.0",
).map(_.exclude("org.slf4j", "slf4j-log4j12"))

libraryDependencies += "com.trueaccord.scalapb" %% "scalapb-runtime" % com.trueaccord.scalapb.compiler.Version.scalapbVersion % "protobuf"

resolvers += "Local Maven Repository" at "file://" + Path.userHome.absolutePath + "/.m2/repository"
resolvers ++= Seq(
  // "Akka Repository" at "http://repo.akka.io/releases/",
  "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
  "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases",
  // "Twitter Maven Repo" at "http://maven.twttr.com",
  "GAEA Depends Repo" at "https://github.com/bmeg/gaia-depends/raw/master/"
)

PB.protoSources in Compile := Seq(new java.io.File("core/src/main/proto"))
PB.targets in Compile := Seq(
  scalapb.gen() -> (sourceManaged in Compile).value
)

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "content/repositories/releases")
}

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

test in assembly := {}

assemblyMergeStrategy in assembly ~= { (old) =>
  {
    case PathList("com", "esotericsoftware", xs @ _*) => MergeStrategy.first
    case PathList("javax", "servlet", xs @ _*) => MergeStrategy.first
    case PathList("org", "w3c", xs @ _*) => MergeStrategy.first
    case PathList("org", "apache", "commons", "logging", xs @ _*) => MergeStrategy.first
    case "about.html"     => MergeStrategy.discard
    case "reference.conf" => MergeStrategy.concat
    case "log4j.properties"     => MergeStrategy.concat
    //case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
    case "META-INF/services/org.apache.hadoop.fs.FileSystem" => MergeStrategy.concat
    case PathList("META-INF", xs @ _*) => MergeStrategy.discard
    case x => MergeStrategy.first
  }
}


