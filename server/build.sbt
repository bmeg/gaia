// import com.trueaccord.scalapb.{ScalaPbPlugin => PB}
// PB.protobufSettings

organization := "bmeg"
name := "gaea"
version := "0.0.1-SNAPSHOT"
scalaVersion := "2.11.8"
resolvers += "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"
javaOptions += "-D-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=9911"

libraryDependencies ++= Seq(
  "com.thinkaurelius.titan" %  "titan-core"          % "1.1.0-SNAPSHOT",
  "com.thinkaurelius.titan" %  "titan-cassandra"     % "1.1.0-SNAPSHOT",
  "com.thinkaurelius.titan" %  "titan-es"            % "1.1.0-SNAPSHOT",
  "com.google.code.gson"    %  "gson"                % "2.6.2",
  "com.google.protobuf"     %  "protobuf-java"       % "3.0.0-beta-2",

  "org.http4s"              %% "http4s-blaze-server" % "0.12.4",
  "org.http4s"              %% "http4s-dsl"          % "0.12.4",
  "org.http4s"              %% "http4s-argonaut"     % "0.12.4",
  "com.michaelpollmeier"    %% "gremlin-scala"       % "3.1.1-incubating.2",
  "org.scala-debugger"      %% "scala-debugger-api"  % "1.0.0"
  // "com.trueaccord.scalapb"  %% "scalapb-json4s"      % "0.1.1"
)

libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.6.4"
