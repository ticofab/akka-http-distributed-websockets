name := "common"
version := "0.0.1"
scalaVersion := "2.12.6"
organization := "io.ticofab"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-cluster" % "2.5.14",
  "org.wvlet.airframe" %% "airframe-log" % "0.51",
  "com.typesafe.akka" %% "akka-http" % "10.1.3"
)
