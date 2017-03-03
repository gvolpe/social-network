name := """social-network"""

version := "1.0"

scalaVersion := "2.11.8"

lazy val titanVersion   = "1.0.0"

libraryDependencies ++= Seq(
  "com.michaelpollmeier" %% "gremlin-scala" % "3.0.2-incubating.2",
  "com.thinkaurelius.titan" %  "titan-core"       % titanVersion,
//  "com.thinkaurelius.titan" %  "titan-cassandra"  % titanVersion,
  "org.scalaz" %% "scalaz-core" % "7.2.9",
  "org.scalaz" %% "scalaz-concurrent" % "7.2.9",
  "ch.qos.logback"  %  "logback-classic" % "1.0.6" % "runtime",
  "org.scalatest"   %% "scalatest"  % "2.2.6" % "test",
  "org.scalacheck"  %% "scalacheck" % "1.12.1" % "test"
)
