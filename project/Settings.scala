import Dependencies._
import sbt.Keys._

object Settings {

  lazy val commonSettings = Seq(
    organization := "com.github.niqdev",
    scalaVersion := V.scala,
    version := "0.1.0"
  )

  lazy val libSettings = commonSettings ++ Seq(
    name := "akka-stream-cassandra",
    libraryDependencies ++= libDependencies
  )

  lazy val exampleSettings = commonSettings ++ Seq(
    name := "example",
    libraryDependencies ++= exampleDependencies,
    mainClass in run := Some("com.github.niqdev.Main")
  )

}
