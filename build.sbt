import Settings._

lazy val lib = project.in(file("lib"))
  .settings(libSettings)

lazy val example = project.in(file("example"))
  .settings(exampleSettings)
  .dependsOn(lib)

lazy val `akka-stream-cassandra` = project.in(file("."))
  .aggregate(lib, example)
