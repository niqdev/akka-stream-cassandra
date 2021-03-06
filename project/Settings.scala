import Dependencies._
import ch.epfl.scala.sbt.release.ReleaseEarlyPlugin.autoImport._
import com.lucidchart.sbt.scalafmt.ScalafmtCorePlugin.autoImport.scalafmtOnCompile
import com.typesafe.sbt.SbtGit.git
import com.typesafe.sbt.SbtPgp.autoImport._
import com.typesafe.sbt.sbtghpages.GhpagesPlugin.autoImport._
import sbt.Keys.{scmInfo, _}
import sbt._
import scoverage.ScoverageKeys._

object Settings {

  lazy val commonSettings = Seq(
    scalaVersion := V.scala,

    organization := "com.github.niqdev",
    organizationName := "niqdev",
    startYear := Some(2017),
    licenses := Seq("MIT" -> url("https://github.com/niqdev/akka-stream-cassandra/blob/master/LICENSE")),

    scalafmtOnCompile in Compile := true,

    mappings in(Compile, packageBin) ~= {
      _.filterNot {
        case (filePath, pathInJar) => pathInJar.endsWith("logback.xml")
      }
    }
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
    developers := List(Developer("niqdev", "niqdev", "niqdev@gmail.com", url("https://github.com/niqdev"))),
    pgpPublicRing := file(".travis/local.pubring.asc"),
    pgpSecretRing := file(".travis/local.secring.asc"),
    releaseEarlyWith := BintrayPublisher,

    git.remoteRepo := scmInfo.value.get.connection,
    excludeFilter in ghpagesCleanSite :=
      new FileFilter {
        def accept(f: File) = (ghpagesRepository.value / "index.html").getCanonicalPath == f.getCanonicalPath
      }
  )

  lazy val exampleSettings = commonSettings ++ Seq(
    name := "example",
    libraryDependencies ++= exampleDependencies,
    mainClass in run := Some("com.github.niqdev.Main")
  )

}
