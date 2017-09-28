# Development

```
# run main
sbt example/run
sbt "example/run-main com.github.niqdev.Main"

# hot reload
sbt ~re-start

# create header manually
sbt headerCreate

# check style
sbt scalastyle

# run test, coverage and generate report
sbt clean coverage test coverageReport

# view coverage report in browser (mac|linux)
open ./lib/target/scala-2.12/scoverage-report/index.html
xdg-open ./lib/target/scala-2.12/scoverage-report/index.html
```

### Documentation

* [Astyanax](https://github.com/Netflix/astyanax)
