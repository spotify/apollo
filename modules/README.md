# Apollo Core Modules

Apollo Core has a module system that simplifies setting up common use-cases
such as a HTTP server, Cassandra connections and so on.

The module system is built on top of Google Guice.  This was deemed to
be the most light-weight module system available for Java that still
supports dynamic configuration (Dagger for example only supports
static configuration).  Apollo-core (aka Leto) extends Guice by making modules
auto-loadable, and configuration-driven.
