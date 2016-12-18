organization := "io.bmeg"
name := "gaia-command"
version := "0.0.3-SNAPSHOT"

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
  "ch.qos.logback"             %  "logback-classic"        % "1.1.2",

  "com.typesafe.scala-logging" %% "scala-logging"          % "3.1.0",
  "org.scala-debugger"         %% "scala-debugger-api"     % "1.0.0",
  "com.lihaoyi"                %% "scalatags"              % "0.6.0",
  "org.rogach"                 %% "scallop"                % "2.0.5",
  // "com.trueaccord.scalapb"  %% "scalapb-json4s"         % "0.1.1"

  "org.scalactic"              %% "scalactic"                % "3.0.0",
  "org.scalatest"              %% "scalatest"                % "3.0.0" % "test"
)

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "content/repositories/releases")
}

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")


mergeStrategy in assembly <<= (mergeStrategy in assembly) { (old) =>
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



