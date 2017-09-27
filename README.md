# akka-stream-cassandra

[![Build Status][travis-image]][travis-url]
[![Download][bintray-image]][bintray-url]

[travis-image]: https://travis-ci.org/niqdev/akka-stream-cassandra.svg?branch=master
[travis-url]: https://travis-ci.org/niqdev/akka-stream-cassandra
[bintray-image]: https://api.bintray.com/packages/niqdev/maven/akka-stream-cassandra/images/download.svg
[bintray-url]: https://bintray.com/niqdev/maven/akka-stream-cassandra/_latestVersion

> TODO work in progress

### Setup

Add the [jcenter](http://jcenter.bintray.com) repository in your build definition as explained [here](http://www.scala-sbt.org/0.13/docs/Resolvers.html)
```
resolvers += Resolver.jcenterRepo
```

Import the latest stable [semver](http://semver.org) version in your project. For more info about versioning please refer to [sbt-release-early](https://github.com/scalacenter/sbt-release-early)
```
libraryDependencies ++= Seq(
  "com.github.niqdev" %% "akka-stream-cassandra" % "x.y.z"
)
```

### Development

```
# run main
sbt example/run
```

<!-- https://github.com/Netflix/astyanax -->
