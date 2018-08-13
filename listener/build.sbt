name := "listener"
version := "0.0.1"
organization := "ticofab.io"
scalaVersion := "2.12.6"

lazy val common = RootProject(file("../common"))
val main = Project(id = "listener", base = file(".")).dependsOn(common)
