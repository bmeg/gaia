// import com.trueaccord.scalapb.{ScalaPbPlugin => PB}
// PB.protobufSettings

organization := "io.bmeg"
name := "gaia-server"
version := "0.0.7-SNAPSHOT"

scalaVersion := "2.11.8"
resolvers += "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"
resolvers += "OSS Sonatype" at "https://repo1.maven.org/maven2/"

resolvers ++= Seq(
  "Akka Repository" at "http://repo.akka.io/releases/",
  "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
  "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases",
  "Twitter Maven Repo" at "http://maven.twttr.com",
  "GAEA Depends Repo" at "https://github.com/bmeg/gaia-depends/raw/master/"
)

val http4sVersion = "0.15.0a"

libraryDependencies ++= Seq(
  "com.google.code.gson"       %  "gson"                   % "2.6.2",
  "ch.qos.logback"             %  "logback-classic"        % "1.1.2",
  "org.apache.kafka"           %  "kafka-clients"          % "0.10.0.1",

  "org.http4s"                 %% "http4s-core"            % http4sVersion,
  "org.http4s"                 %% "http4s-blaze-server"    % http4sVersion,
  "org.http4s"                 %% "http4s-dsl"             % http4sVersion,
  "org.http4s"                 %% "http4s-argonaut"        % http4sVersion,
  "com.typesafe.scala-logging" %% "scala-logging"          % "3.1.0",
  "org.scala-debugger"         %% "scala-debugger-api"     % "1.0.0",
  "com.github.alexarchambault" %% "argonaut-shapeless_6.1" % "1.1.1",
  "com.lihaoyi"                %% "scalatags"              % "0.6.0",

  "org.scalactic"              %% "scalactic"              % "3.0.0",
  "org.scalatest"              %% "scalatest"              % "3.0.0" % "test"
)

// libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.6.4"

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "content/repositories/releases")
}

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")


assemblyMergeStrategy in assembly ~= { (old) =>
  {
    case PathList("com", "esotericsoftware", xs @ _*) => MergeStrategy.first
    case PathList("javax", "servlet", xs @ _*) => MergeStrategy.first
    case PathList("org", "w3c", xs @ _*) => MergeStrategy.first
    case PathList("org", "apache", "commons", "logging", xs @ _* ) => MergeStrategy.first
    case "about.html"     => MergeStrategy.discard
    case "reference.conf" => MergeStrategy.concat
    case "log4j.properties"     => MergeStrategy.concat
    //case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
    case "META-INF/services/org.apache.hadoop.fs.FileSystem" => MergeStrategy.concat
    case PathList("META-INF", xs @ _*) => MergeStrategy.discard
    case x => MergeStrategy.first
  }
}



