name := "udemy-akka-streams"

version := "0.1"

scalaVersion := "2.12.9"

lazy val akkaVersion = "2.5.25"
lazy val scalaTestVersion = "3.0.8"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion,
  "org.scalatest" %% "scalatest" % scalaTestVersion
)