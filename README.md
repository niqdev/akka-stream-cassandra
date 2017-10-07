# akka-stream-cassandra

[![Build Status][travis-image]][travis-url]
[![Coverage Status][coveralls-image]][coveralls-url]
[![Download][bintray-image]][bintray-url]
[![Scaladoc][scaladoc-image]][scaladoc-url]

[travis-image]: https://travis-ci.org/niqdev/akka-stream-cassandra.svg?branch=master
[travis-url]: https://travis-ci.org/niqdev/akka-stream-cassandra
[coveralls-image]: https://coveralls.io/repos/github/niqdev/akka-stream-cassandra/badge.svg?branch=master
[coveralls-url]: https://coveralls.io/github/niqdev/akka-stream-cassandra?branch=master
[bintray-image]: https://api.bintray.com/packages/niqdev/maven/akka-stream-cassandra/images/download.svg
[bintray-url]: https://bintray.com/niqdev/maven/akka-stream-cassandra/_latestVersion
[scaladoc-image]: https://img.shields.io/badge/scaladoc-online-orange.svg
[scaladoc-url]: https://goo.gl/X6e6M2

Convenient [stream](https://doc.akka.io/docs/akka/2.5.6/scala/stream/index.html) based library
for unsupported and no longer maintained versions of Cassandra and [DataStax Enterprise](http://docs.datastax.com/en/archived/cassandra/1.2/index.html)

Please prefer [alpakka](https://developer.lightbend.com/docs/alpakka/current/cassandra.html) Cassandra Connector for version 2 and above

### Setup

Add the [jcenter](http://jcenter.bintray.com) repository in your build definition and import the latest stable version
```scala
resolvers += Resolver.jcenterRepo

libraryDependencies ++= Seq(
  "com.github.niqdev" %% "akka-stream-cassandra" % "0.7.1"
    // you might prefer to use a different version
    exclude("com.netflix.astyanax", "astyanax")
)
```

### Example

```scala
implicit val actorSystem: ActorSystem = ActorSystem("actor-system")
implicit val materializer: ActorMaterializer = ActorMaterializer()
implicit val executionContext: ExecutionContext = actorSystem.dispatcher

val keyspace: Keyspace = ...
val columnFamily: ColumnFamily[String, String] = ...

CassandraSource(keyspace, columnFamily)
  .via(...)
  .runForeach { row =>
    log.debug(s"row: ${row.getKey}")
  }
```
Refer to the [test](lib/src/test/scala/com/github/niqdev/stream/CassandraSourceSpec.scala) for a full example with [EmbeddedCassandra](https://github.com/Netflix/astyanax/search?q=EmbeddedCassandra)
