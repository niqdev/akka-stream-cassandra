### C* migration
with Akka Stream

---

Migrate from
### DSE 3.1.6 (Cassandra 1.2)
to
### Cassandra 3.11

+++

*Why?*

- it's the latest version!
- EOSL i.e. patches/bug fixes and support are not available
- get rid of DSE license fees
- solve scaling issues e.g. vnodes
- and more ...

+++

*How?*

| Step | Old C *Read* | Old C *Write* | New C *Read* | New C *Insert* | New C *Update* | Job |

| 1 | **ON** | **ON** | OFF | OFF | OFF | BEFORE |
| 2 | ON | ON | OFF | OFF | **ON** | BEFORE |
| 3 | ON | ON | OFF | **ON** | ON | BEFORE |
| 4 | ON | ON | OFF | ON | ON | **RUN** |
| 5 | ON | ON | **ON** | ON | ON | AFTER |
| 6 | **OFF** | ON | ON | ON | ON | AFTER |
| 7 | OFF | **OFF** | ON | ON | ON | AFTER |

---

TODO
