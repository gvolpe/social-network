name := """social-network"""

version := "1.0"

scalaVersion := "2.11.8"

lazy val circeVersion   = "0.5.1"
lazy val http4sVersion  = "0.14.6"
lazy val titanVersion   = "1.0.0"

libraryDependencies ++= Seq(
  "org.http4s"      %% "http4s-dsl"           % http4sVersion,
  "org.http4s"      %% "http4s-blaze-server"  % http4sVersion,
  "org.http4s"      %% "http4s-blaze-client"  % http4sVersion,
  "org.http4s"      %% "http4s-circe"         % http4sVersion,
  "io.circe"        %% "circe-core"           % circeVersion,
  "io.circe"        %% "circe-generic"        % circeVersion,
  "io.circe"        %% "circe-parser"         % circeVersion,
  "com.michaelpollmeier" %% "gremlin-scala" % "3.0.2-incubating.2",
  "com.thinkaurelius.titan" %  "titan-core"       % titanVersion,
//  "com.thinkaurelius.titan" %  "titan-cassandra"  % titanVersion,
  "ch.qos.logback" % "logback-classic" % "1.0.6" % "runtime",
  "org.scalatest"   %% "scalatest"  % "2.2.6" % "test",
  "org.scalacheck"  %% "scalacheck" % "1.12.1" % "test"
)
