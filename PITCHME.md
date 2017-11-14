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

https://github.com/Netflix/astyanax

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

Main

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

https://doc.akka.io/docs/akka/current/scala/actors.html

+++

Actor

```
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

Source (part 1)

```
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
      })
      // ...
}}}}
```

@[1-4, 6, 17]
@[5, 7-16]

https://doc.akka.io/docs/akka/current/scala/stream/stream-customize.html

+++

Source (part 2)

```
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
      // ...
     }})
}}
```

@[1-4, 7-8, 15-16]
@[5, 9-15]

+++

Flow

```
trait MigrationStream {
 // ...
 def convertNewEntity(oldEntity: OldEntity): Try[NewEntity] =
  Try(NewEntityConverter.convert(oldEntity))
 def convertNewEntityFlow[I, O](converter: I => Try[O]):
   Flow[Either[LeftMetadata, I], Either[LeftMetadata, O], NotUsed] =
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

@[1, 15]
@[5-6]
@[7-8, 13-14]
@[3-4, 9-12](converter: I => Try[O])

https://doc.akka.io/docs/akka/current/scala/stream/stream-flows-and-basics.html

+++

TODO Monitor (part 1)

```
def monitorEventFlow[E <: Entity](monitorActor: ActorRef)(implicit ...):
 Flow[Either[LeftMetadata, E], Either[LeftMetadata, E], _] = Flow.fromGraph {
  GraphDSL.create() { implicit builder => import GraphDSL.Implicits._

   val broadcast = builder.add(Broadcast[Either[LeftMetadata, E]](2))
   val zipper = builder.add(Zip[Either[LeftMetadata, E], FlowControl])
   val outputFlow = builder.add(Flow[Either[LeftMetadata, E]])
   
   broadcast.out(0) ~> zipper.in0
   broadcast.out(1) ~> entityToEventFlow ~> monitorEventActorFlow(monitorActor)
                    ~> controlDynamicFlow ~> zipper.in1
   zipper.out.map(_._1) ~> outputFlow.in
   FlowShape(broadcast.in, outputFlow.out)
  }
}
```

https://doc.akka.io/docs/akka/current/scala/stream/stream-graphs.html

+++

TODO Monitor (part 2)

```
package object stream {
 sealed trait FlowControl
 case class Throttle(sleepMillis: Long) extends FlowControl
 case object Continue extends FlowControl
} 
def controlDynamicFlow: Flow[FlowControl, FlowControl, NotUsed] =
 Flow[FlowControl] flatMapConcat {
  case c@Continue =>
   Source.single(c)
  case t@Throttle(sleepMillis) =>
   Source.single(t).delay(sleepMillis, DelayOverflowStrategy.backpressure)
}
```

+++

TODO Parallelism

```
class EntityActor(monitorActor: ActorRef, ...)(implicit ...)
  extends Actor with MigrationStream {
 override def receive: Receive = {
  case Migrate =>
   CassandraSource(...).async
    .via(simple1AsyncFlow).async
    .via(simple1AsyncFlow).async
    .via(customExecutionContextFlow(...,
      actorSystem.dispatchers.lookup("application.custom-dispatcher")).async
    .runWith(...)
    .onComplete { ... }
  }
}
```

@[1-4, 12-13]
@[5-9]

https://doc.akka.io/docs/akka/current/scala/dispatchers.html

+++

`akka-stream-testkit`

https://doc.akka.io/docs/akka/current/scala/stream/stream-testkit.html

+++

Benefits
- any step can fail: `Either[LeftMetadata, T]`
- simple to test with `akka-stream-testkit`
- DRY: easy to abstract and reuse streams
- back-pressure
- use `async` + custom `dispatcher`

---

Resources

TODO

---

## Thanks!

### Any Questions?
