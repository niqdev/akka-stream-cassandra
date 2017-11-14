### C* migration
with Akka Stream

---

Migrate from
### Cassandra 1.2 (DSE 3.1.6)
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
- substitute the facade with the new lib
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

```scala
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

```scala
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

Main

```scala
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

Actor

```scala
class EntityActor(monitorActor: ActorRef, ...)(implicit ...)
  extends Actor with MigrationStream {
 override def receive: Receive = {
  case Migrate =>
   CassandraSource(...)
    .via(convertRowFlow)
    .via(convertOldEntityFlow)
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

Package

```scala
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

Source (part 1)

```scala
class CassandraSource[K, C](...)(implicit ...)
  extends GraphStage[SourceShape[Row[K, C]]] {
 override def createLogic(inheritedAttributes: Attributes) =
  new GraphStageLogic(shape) {
   val queue = new LinkedBlockingQueue[Row[K, C]](queueSize)
   override def preStart(): Unit = Future {
    new AllRowsReader.Builder[K, C](keyspace, columnFamily)
      .withPageSize(pageSize)
      .forEachRow(new com.google.common.base.Function
                      [Row[K, C], java.lang.Boolean] {
        override def apply(input: Row[K, C]) = {
         queue.put(input)
         true
        }
      }).build().call()
}}}
```

@[1-4, 6, 16]
@[5, 7-15]

+++

Source (part 2)

```scala
class CassandraSource[K, C](...)(implicit ...)
  extends GraphStage[SourceShape[Row[K, C]]] {
 override def createLogic(inheritedAttributes: Attributes) =
  new GraphStageLogic(shape) {
   val queue = new LinkedBlockingQueue[Row[K, C]](queueSize)
   // ...
   setHandler(out, new OutHandler {
    override def onPull(): Unit =
     Try(queue.poll(dequeueTimeout, TimeUnit.SECONDS)) match {
      case Success(input) if Option(input).isDefined =>
       push(out, input)
      case Failure(e) =>
       complete(out)
     }})
}}
```

@[1-4, 7-8, 14-15]
@[5, 9-14]

+++

Flow

```scala
trait MigrationStream {
 def convertNewEntity(oldEntity: OldEntity): Try[NewEntity] =
  Try(NewEntityConverter.convert(oldEntity))
 def convertNewEntityFlow[I, O](converter: I => Try[O]):
   Flow[Either[LeftMetadata,I],Either[LeftMetadata,O],NotUsed] =
  Flow[Either[LeftMetadata, I]] map {
   case Right(entity) =>
    converter(entity) match {
     case Success(output) => Right(output)
     case Failure(error) => Left(ErrorEvent, s"Error: $error")
    }
   case left => left
  }
}
```

@[1, 14]
@[4-5]
@[6-7, 12-13]
@[2-3, 8-11]

+++

Monitor (part 1)

```scala
package object stream {
 sealed trait FlowControl
 case class Throttle(sleepMillis: Long) extends FlowControl
 case object Continue extends FlowControl
}
trait MonitorStream {
 def controlDynamicFlow: Flow[FlowControl,FlowControl,NotUsed] =
  Flow[FlowControl] flatMapConcat {
   case c@Continue =>
    Source.single(c)
   case t@Throttle(sleepMillis) =>
    Source.single(t)
     .delay(sleepMillis, DelayOverflowStrategy.backpressure)
  }
}
```

@[1-5]
@[6-7, 15]
@[8-14]

+++

Monitor (part 2)

```scala
def monitorEventFlow[E](monitorActor: ActorRef)(implicit ...):
  Flow[Either[LeftMetadata, E], Either[LeftMetadata, E], _] =
 Flow.fromGraph { GraphDSL.create() {
   implicit b => import GraphDSL.Implicits._
   val broadcast = b.add(Broadcast[Either[LeftMetadata, E]](2))
   val zipper = b.add(Zip[Either[LeftMetadata, E], FlowControl])
   val outputFlow = b.add(Flow[Either[LeftMetadata, E]])
   broadcast.out(0) ~> zipper.in0
   broadcast.out(1) ~> entityToEventFlow
                    ~> monitorEventActorFlow(monitorActor)
                    ~> controlDynamicFlow
                    ~> zipper.in1
   zipper.out.map(_._1) ~> outputFlow.in
   FlowShape(broadcast.in, outputFlow.out)
}}
```

@[1-3, 15]
@[4-7, 14]
@[8-13]

+++

Parallelism

```scala
class EntityActor(monitorActor: ActorRef, ...)(implicit ...)
  extends Actor with MigrationStream {
 override def receive: Receive = {
  case Migrate =>
   CassandraSource(...).async
    .via(simpleAsyncFlow).async
    .via(customExecutionContextFlow(
      actorSystem.dispatchers
                 .lookup("application.custom-dispatcher"))
                 .async
    .runWith(...)
    .onComplete { ... }
  }
}
```

@[1-4, 13-14]
@[5-12]

+++

`akka-stream-testkit`

+

`ScalaTest`

+++

Benefits
- any step can fail: `Either[LeftMetadata, T]`
- simple to test
- DRY: easy to abstract and reuse streams
- use `async` + custom `dispatcher`
- back-pressure
- it's fun!

---

Resources

- [Astyanax](https://github.com/Netflix/astyanax)
- [Datastax](http://docs.datastax.com/en/landing_page/doc/landing_page/docList.html)
- [Akka](https://doc.akka.io/docs/akka/current/scala/index.html)
- [ScalaTest](http://www.scalatest.org)

---

## Thanks!

### Any Questions?
