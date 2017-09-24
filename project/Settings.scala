import Dependencies._
import ch.epfl.scala.sbt.release.ReleaseEarlyPlugin.autoImport._
import com.typesafe.sbt.SbtPgp.autoImport._
import sbt.Keys._
import sbt._

object Settings {

  lazy val commonSettings = Seq(
    organization := "com.github.niqdev",
    scalaVersion := V.scala
  )

  lazy val libSettings = commonSettings ++ Seq(
    name := "akka-stream-cassandra",
    libraryDependencies ++= libDependencies,
    licenses := Seq("MIT" -> url("https://opensource.org/licenses/MIT")),
    homepage := Some(url(s"https://github.com/niqdev/akka-stream-cassandra")),
    scmInfo := Some(
      ScmInfo(url(s"https://github.com/niqdev/akka-stream-cassandra"),
        "scm:git:git@github.com:niqdev/akka-stream-cassandra.git")),
    pgpPublicRing := file("./travis/local.pubring.asc"),
    pgpSecretRing := file("./travis/local.secring.asc"),
    releaseEarlyWith := BintrayPublisher
  )

  lazy val exampleSettings = commonSettings ++ Seq(
    name := "example",
    libraryDependencies ++= exampleDependencies,
    mainClass in run := Some("com.github.niqdev.Main")
  )

}
