import Settings._

lazy val lib = project.in(file("lib"))
  .settings(libSettings)
  .settings(
    licenses := Seq("MIT" -> url("https://github.com/niqdev/akka-stream-cassandra/blob/master/LICENSE")),
    homepage := Some(url(s"https://github.com/niqdev/akka-stream-cassandra")),
    scmInfo := Some(
      ScmInfo(url(s"https://github.com/niqdev/akka-stream-cassandra"),
        "scm:git:git@github.com:niqdev/akka-stream-cassandra.git")),
    developers := List(Developer("niqdev", "niqdev", "niqdev@gmail.com", url("https://github.com/niqdev/akka-stream-cassandra"))),
    pgpPublicRing := file("./travis/local.pubring.asc"),
    pgpSecretRing := file("./travis/local.secring.asc"),
    releaseEarlyWith := BintrayPublisher
  )

lazy val example = project.in(file("example"))
  .settings(exampleSettings)
  .dependsOn(lib)

lazy val `akka-stream-cassandra` = project.in(file("."))
  .aggregate(lib, example)
