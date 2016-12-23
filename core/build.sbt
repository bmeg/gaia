organization  := "io.bmeg"
name := "gaia-core"

version := "0.0.4-SNAPSHOT"

scalaVersion := "2.11.8"

conflictManager := ConflictManager.strict.copy(organization = "com.esotericsoftware.*")

libraryDependencies ++= Seq(
  "com.thinkaurelius.titan"    %  "titan-core"               % "1.1.0-SNAPSHOT",
  "com.thinkaurelius.titan"    %  "titan-cassandra"          % "1.1.0-SNAPSHOT",
  "com.thinkaurelius.titan"    %  "titan-es"                 % "1.1.0-SNAPSHOT",
  "org.apache.tinkerpop"       %  "gremlin-core"             % "3.1.1-incubating",
  "com.google.protobuf"        %  "protobuf-java"            % "3.1.0",
  "com.google.protobuf"        %  "protobuf-java-util"       % "3.1.0",
  "com.google.protobuf"        %  "protoc"                   % "3.1.0",
  "org.scala-lang"             %  "scala-compiler"           % "2.11.8",
  "com.fasterxml.jackson.module" %% "jackson-module-scala"    % "2.8.4",
  // "org.slf4j"                  %  "slf4j-nop"                % "1.7.21",

  // "org.json4s"                 %% "json4s-native"            % "3.3.0",
  // "org.json4s"                 %% "json4s-jackson"           % "3.3.0",
  "com.michaelpollmeier"       %% "gremlin-scala"            % "3.1.2-incubating.0",
  
  "org.apache.kafka"           %  "kafka-clients"            % "0.10.0.1",
  "net.manub"                  %% "scalatest-embedded-kafka" % "0.7.1" % "test",
  "net.jcazevedo"              %% "moultingyaml"             % "0.3.0",
  "io.bmeg"                    %% "ophion"                   % "0.0.2-SNAPSHOT",

  "org.scalactic"              %% "scalactic"                % "3.0.0",
  "org.scalatest"              %% "scalatest"                % "3.0.0" % "test"
).map(_.exclude("org.slf4j", "slf4j-log4j12"))

resolvers += "Local Maven Repository" at "file://" + Path.userHome.absolutePath + "/.m2/repository"
resolvers ++= Seq(
  "Akka Repository" at "http://repo.akka.io/releases/",
  "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
  "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases",
  "Twitter Maven Repo" at "http://maven.twttr.com",
  "GAEA Depends Repo" at "https://github.com/bmeg/gaia-depends/raw/master/"
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

mergeStrategy in assembly <<= (mergeStrategy in assembly) { (old) =>
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


