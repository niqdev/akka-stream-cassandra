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

- lib for each entity to handle new C* version
- facade to wrap old and new C* libraries
- integrate facade in each service to manage read/write flags on both C*
- substitute the facade with the new lib
- drop tables in old C*

---

TODO
