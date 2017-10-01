import Settings._

lazy val lib = project.in(file("lib"))
  .settings(libSettings)
  .enablePlugins(AutomateHeaderPlugin, SiteScaladocPlugin, GhpagesPlugin)

lazy val example = project.in(file("example"))
  .settings(exampleSettings)
  .enablePlugins(AutomateHeaderPlugin)
  .dependsOn(lib)

lazy val `akka-stream-cassandra` = project.in(file("."))
  .disablePlugins(RevolverPlugin)
  .aggregate(lib, example)
