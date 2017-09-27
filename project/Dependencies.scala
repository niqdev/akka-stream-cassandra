import sbt._

object Dependencies {

  lazy val V = new {
    val scala = "2.12.3"

    val logback = "1.2.3"
    val scalaLogging = "3.7.2"
    val config = "1.3.1"

    val scalatest = "3.0.4"
  }

  lazy val commonDependencies = Seq(
    "ch.qos.logback" % "logback-classic" % V.logback,
    "com.typesafe.scala-logging" %% "scala-logging" % V.scalaLogging,
    "com.typesafe" % "config" % V.config
  )

  lazy val testDependencies = Seq(
    "org.scalatest" %% "scalatest" % V.scalatest % Test
  )

  lazy val libDependencies = commonDependencies ++ testDependencies

  lazy val exampleDependencies = commonDependencies

}
