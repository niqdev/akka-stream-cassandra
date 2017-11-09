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
- EOSL i.e. patches/bug fixes and support are not available
- get rid of DSE license fees
- solve scaling issues e.g. vnodes
- and more ...

+++

*How*

- lib for each entity to handle new C* version
- facade to wrap old and new C* libraries
- integrate facade in each service to manage read/write flags on both C*
- substitute the facade with the new lib
- drop tables in old C*

---

*Job*

- standalone scala app
- reads all rows from old C*
- converts from old to new entity model
- basic validation
- stores in new cassandra
- reads again from old cassandra to verify possible race conditions
- dynamic throttling

---

Java Style

```
abstract class Migration(...) {
  val pageSize = 1000
  lazy val astyanaxClient = ???
  lazy val datastaxClient = ???

  // guava
  def rowFunction: com.google.common.base.Function[Row[String, String], java.lang.Boolean]

  def migrate: Unit = {
    val astyanaxKeyspace: Keyspace = ???
    val columnFamily: ColumnFamily[String, String] = ???

    // ...
    
    new AllRowsReader.Builder[String, String](astyanaxKeyspace, columnFamily)
      .withPageSize(pageSize)
      .forEachRow(rowFunction)
      .build()
      .call()
    
    // ...
  }
}
```

@[1, 9, 22-23]
@[6-7, 15-19]
