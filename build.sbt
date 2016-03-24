import com.trueaccord.scalapb.{ScalaPbPlugin => PB}

PB.protobufSettings

organization := "bmeg"
name := "gaea"
version := "0.0.1-SNAPSHOT"
scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "org.http4s"              %% "http4s-blaze-server" % "0.12.4",
  "org.http4s"              %% "http4s-dsl"          % "0.12.4",
  "org.http4s"              %% "http4s-argonaut"     % "0.12.4",
  "com.thinkaurelius.titan" %  "titan-core"          % "1.0.0",
  "com.thinkaurelius.titan" %  "titan-cassandra"     % "1.0.0",
  "com.thinkaurelius.titan" %  "titan-es"            % "1.0.0"
)

libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.6.4"
