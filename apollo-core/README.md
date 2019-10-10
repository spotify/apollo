# Apollo Core (a.k.a. Leto)

The apollo-core library manages the lifecycle (loading, starting, and stopping) of your service on
production machines. You do not usually need to interact directly with apollo-core. Apollo Core
doesn't enforce any service architecture, so it can be used standalone, within a Java Servlet, in a
batch command, in a CLI tool, etc.

## Simple usage

The simplest possible service that uses Apollo Core looks like:

```java
package com.spotify.apollo.example;

import com.spotify.apollo.core.Service;
import com.spotify.apollo.core.Services;

import java.io.IOException;

public class App {

  public static void main(String... args) throws IOException {
    Service service = Services.usingName("test").build();
    Services.run(service, args);
  }
}
```

## Life-cycle control

To gain control over the Apollo Core lifecycle, simply manage it yourself:

```java
Service service = Services.usingName("test").build();

// Before service started
try (Service.Instance instance = service.start(args)) {
  // After service started

  instance.waitForShutdown();

  // Before service stopped
} catch (Exception e) {
  // Service crashed while running
} finally {
  // After service stopped
}
// After successful service run
```

## Features

Apollo Core provides a standard configuration for Java services.  It tries to
be API-compatible with Apollo, and adds additional features that help
solve some inconsistencies in Apollo, Helios, and other Spotify
support libraries.

The API is summarized in the
[Apollo Core service interface](src/main/java/com/spotify/apollo/core/Service.java).

### Consistent configuration

A core principle of Apollo Core is that everything should be configured via
the service configuration.  No environment variables, weird
command-line options or oracles.  Apollo Core then defines a standard way of
controlling this configuration (and mapping environment variables to
configuration in the situations when it's necessary).

```java
Service service = Services.usingName("test").build();

try (Service.Instance instance = service.start(args)) {
  instance.getConfig(); // typesafe.config
}
```

Apollo Core will look for a configuration file with the name
`<service-name>.conf` on the classpath. `test.conf` in the example above.
It is possible to overlay an explicit config file on top of the classpath
resolved config by using the command line argument `--config <config-file>`.

Apollo Core adds support for overriding configuration keys
using `-D` command-line options.  For example, `-Ddomain=example.org`
sets the `domain` config key to `example.org`.

Apollo Core also considers environment variables of the form
`APOLLO_X_Y=ab`, where the `APOLLO` prefix is [configurable with `Service.Builder.withEnvVarPrefix()`](src/main/java/com/spotify/apollo/core/Service.java).
These are translated into configuration keys of the form `x.y=ab`.
The values of the environment variables are treated as strings and
copied verbatim; no special syntax is supported.

### Logging configuration

NOTE/TODO: a logging module is currently not included
Apollo Core sets up logging for you.  Some supported ways of doing that:

| Argument                 | Impact                             |
|--------------------------|------------------------------------|
| `-v`, `-vv`, `--verbose` | Increases amount of log output     |
| `-c`, `-cc`, `--concise` | Decreases amount of log output     |
| `-q`, `--quiet`          | Outputs no log                     |
| `--syslog[=true/false]`  | Enables/disables logging to syslog |

All of these options map directly to configuration keys.

### Scoped executors

Many services are polluted by executors being allocated everywhere.
It is not uncommon to have many hundred threads in an application.
Most of the time, an application can instead use a limited set of
threads that are managed by central executor services.  This also
makes it possible to avoid having to use daemon threads, which are
[known to have issues](doc/daemon-threads.md) when shutting down the
JVM.  Apollo Core manages shared executors that are general purpose for just
that reason.

```java
Service service = Services.usingName("test").build();

try (Service.Instance instance = service.start(args)) {
  instance.getExecutorService(); // For long-running jobs
  instance.getScheduledExecutorService(); // For periodical, short-lived jobs
}
```

### Managed application clean-up

Every service will have resources that need to be freed before the
application exits.  It is very hard to do this correctly if you
manually have to control the clean-up.  A common problem is for
example that if two resources `a` and `b` need to be cleaned up, and
`a.close()` throws an exception, the clean-up of `b` never happens.

Apollo Core exposes an API that lets you delegate clean-up to the life cycle
manager, which will do the right thing:

```java
Service service = Services.usingName("test").build();

try (Service.Instance instance = service.start(args)) {
  // Something convoluted that prevents you from doing try-with-resources
  Configuration c = loadConfigFromZK();

  Connection connection = connectionFactory.connect(c);

  // Guaranteed to call close() before application quits
  instance.getCloser().register(connection);

  instance.waitForShutdown();
}
```

### Delegating command-line configuration

When implementing a command-line tool, that tool typically needs to do
some command-line parsing on its own.  Apollo Core conforms to the UNIX/POSIX
tool behavior and preserves unprocessed command-line options similar
to `getopt(3)`.

```java
Service service = Services.usingName("test").build();

try (Service.Instance instance = service.start(args)) {
  instance.getUnprocessedArgs(); // Unrecognized command-line args
}
```

## Modules

Apollo Core has a module system that simplifies setting up common use-cases
such as a HTTP server, Cassandra connections and so on.

The module system is built on top of Google Guice.  This was deemed to
be the most light-weight module system available for Java that still
supports dynamic configuration (Dagger for example only supports
static configuration).  Apollo Core extends Guice by making modules
auto-loadable, and configuration-driven.

Modules can be found under [modules](../modules):

* [Http Server](../modules/jetty-http-server)
* [Http Client](../modules/okhttp-client)
