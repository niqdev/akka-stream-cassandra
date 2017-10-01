# Development

```
# run main
sbt example/run
sbt "example/run-main com.github.niqdev.Main"

# debug test (remote)
sbt clean test -jvm-debug 5005

# hot reload
sbt ~re-start

# format code
sbt scalafmt

# add header
sbt headerCreate

# check style
sbt scalastyle

# run test, coverage and generate report
sbt clean coverage test coverageReport

# view coverage report in browser (mac|linux)
open ./lib/target/scala-2.12/scoverage-report/index.html
xdg-open ./lib/target/scala-2.12/scoverage-report/index.html

# show project dependencies
sbt dependencyTree

# verify dependencies
sbt "whatDependsOn ch.qos.logback logback-classic 1.2.3"

# show outdated dependencies
sbt dependencyUpdates
```

### Documentation

* [Astyanax](https://github.com/Netflix/astyanax)
