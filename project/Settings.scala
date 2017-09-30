import Dependencies._
import ch.epfl.scala.sbt.release.ReleaseEarlyPlugin.autoImport._
import com.lucidchart.sbt.scalafmt.ScalafmtCorePlugin.autoImport.scalafmtOnCompile
import com.typesafe.sbt.SbtPgp.autoImport._
import sbt.Keys._
import sbt._
import scoverage.ScoverageKeys._

object Settings {

  lazy val commonSettings = Seq(
    scalaVersion := V.scala,

    organization := "com.github.niqdev",
    organizationName := "niqdev",
    startYear := Some(2017),
    licenses := Seq("MIT" -> url("https://github.com/niqdev/akka-stream-cassandra/blob/master/LICENSE")),

    scalafmtOnCompile in Compile := true
  )

  lazy val libSettings = commonSettings ++ Seq(
    name := "akka-stream-cassandra",
    libraryDependencies ++= libDependencies,

    coverageMinimum := 70,
    coverageFailOnMinimum := true,

    homepage := Some(url(s"https://github.com/niqdev/akka-stream-cassandra")),
    scmInfo := Some(
      ScmInfo(url(s"https://github.com/niqdev/akka-stream-cassandra"),
        "scm:git:git@github.com:niqdev/akka-stream-cassandra.git")),
    developers := List(Developer("niqdev", "niqdev", "niqdev@gmail.com", url("https://github.com/niqdev/akka-stream-cassandra"))),
    pgpPublicRing := file(".travis/local.pubring.asc"),
    pgpSecretRing := file(".travis/local.secring.asc"),
    releaseEarlyWith := BintrayPublisher
  )

  lazy val exampleSettings = commonSettings ++ Seq(
    name := "example",
    libraryDependencies ++= exampleDependencies,
    mainClass in run := Some("com.github.niqdev.Main")
  )

}
