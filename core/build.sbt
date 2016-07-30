organization  := "bmeg"

name := "gaea-core"

version := "0.0.1-SNAPSHOT"

scalaVersion := "2.11.8"

conflictManager := ConflictManager.strict.copy(organization = "com.esotericsoftware.*")

libraryDependencies ++= Seq(
  "com.thinkaurelius.titan"    % "titan-core"           % "1.1.0-SNAPSHOT",
  "com.thinkaurelius.titan"    % "titan-cassandra"      % "1.1.0-SNAPSHOT",
  "org.apache.tinkerpop"       % "gremlin-core"         % "3.1.1-incubating",
  "com.google.protobuf"        % "protobuf-java"        % "3.0.0-beta-2",
  "com.google.protobuf"        % "protoc"               % "3.0.0-beta-2",
  "org.scala-lang"             % "scala-compiler"       % "2.11.8",

  "org.scalanlp"               %% "breeze"              % "0.12",
  "org.scalanlp"               %% "breeze-natives"      % "0.12",
  "com.michaelpollmeier"       %% "gremlin-scala"       % "3.1.2-incubating.0"

)

//resolvers += "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"

resolvers ++= Seq(
  "Akka Repository" at "http://repo.akka.io/releases/",
  "Sonatype Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots",
  "Sonatype Releases" at "http://oss.sonatype.org/content/repositories/releases",
  "Twitter Maven Repo" at "http://maven.twttr.com",
  "GAEA Depends Repo" at "https://github.com/bmeg/gaea-depends/raw/master/"
)
