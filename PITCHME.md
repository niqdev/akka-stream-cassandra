### C* migration
with Akka Stream

---

Migrate from
### DSE 3.1.6 (Cassandra 1.2)
to
### Cassandra 3.11

+++

*Why*

- it's the latest version!
- EOSL i.e. patches/bug fixes and support are not available anymore
- get rid of DSE license fees
- solve scaling issues (vnodes)
- and more ...

+++

*How*

- lib for each entity to handle new C* version
- wrap old and new C* libraries in facade
- integrate facade in each service to manage read/write flags on both C*
- run job to migrate from old to new C*
- substitute the facade in the services with the new lib
- drop tables in old C*

---

*Job*

- standalone Scala app
- reads all rows from old C*
- converts from old to new entity model
- basic validation
- stores in new cassandra
- reads again from old cassandra to verify possible race conditions
- dynamic throttling

---

Initial approach

**Java Style**

+++

```
abstract class Migration(...) {
 val pageSize = 1000
 def rowFunction: com.google.common.base.Function
                  [Row[String, String], Boolean]
 def migrate: Unit = {
  val astyanaxKeyspace: Keyspace = ???
  val columnFamily: ColumnFamily[String, String] = ???
  new AllRowsReader.Builder[String, String]
                           (astyanaxKeyspace, columnFamily)
   .withPageSize(pageSize)
   .forEachRow(rowFunction)
   .build()
   .call()
 }
}
```

@[1, 5, 14-15]
@[3-4, 8-13]

+++

```
class Job(...) extends Migration(...) {
 override def rowFunction = new RowFunction
}
class RowFunction extends com.google.common.base.Function
                          [Row[String, String], Boolean] {
 override def apply(row: Row[String, String]): Boolean = {
  // 1) convert Row to OldEntity
  // 2) convert OldEntity to NewEntity
  // 3) validate NewEntity
  // 4) check race conditions
  // 5) increment counters SUCCESS/WARNING/ERROR
  // 6) dynamic throttling (read from file and sleep)
  true
 }
}
```

@[1-6, 13-15]
@[7-12]

+++

Issues
- any step can fail: try/catch approach
- hard to test
- not reusable
- low control: sleep is blocking

---

A better approach

**Akka Stream**

+++

```
class Job(...) extends BaseMigration(...) {
 implicit val actorSystem: ActorSystem = ???
 implicit val materializer: ActorMaterializer = ???
 implicit val executionContext: ExecutionContext = ???
 implicit val timeout: Timeout = ???

 val monitorActor: ActorRef = ???
 val entityActor: ActorRef = ???

 override def migrate: Unit =
  actorSystem.scheduler.scheduleOnce
              (10.seconds, entityActor, EntityActor.Migrate)
}
```

@[1, 10, 13]
@[2-5, 7-8]
@[10-12]

+++

```
class EntityActor(monitorActor: ActorRef, ...)(implicit ...)
  extends Actor with MigrationStream {
 override def receive: Receive = {
  case Migrate =>
   CassandraSource(...)
    .via(convertRowFlow)
    .via(filterRowFlow(...))
    .via(convertOldEntityFlow)
    .via(skipInvalidOldEntityFlow)
    .via(convertNewEntityFlow)
    .via(storeNewEntityFlow)
    .via(validateNewEntityFlow)
    .via(monitorEventFlow(monitorActor))
    .runWith(Sink.ignore)
    .onComplete { ... }
  }
}
```

@[1-4, 16-17]
@[5-15]

+++

```
package object stream {
 type AstyanaxRow = Row[String, String]
 type LeftMetadata = (Event, String)

 sealed trait Event
 case object SuccessEvent extends Event
 case object WarningEvent extends Event
 case object ErrorEvent extends Event
}
```

+++

TODO CassandraSource
