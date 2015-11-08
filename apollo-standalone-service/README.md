Apollo Standalone Service
=========================

The `apollo-standalone` library is a small bundle of Apollo modules. It incorporates both
[apollo-api](apollo-api) and [apollo-core](apollo-core) and ties them together with several other
standard modules to make a complete service.

Apollo-standalone gives you what you need to start your backend service. Here, for example, we tell
`apollo-standalone` to boot a service named `"ping"`, defined by the function `Ping::init`, and
handle any command line arguments passed in through the `args` variable:

```java
public static void main(String[] args) throws LoadingException {
  StandaloneService.boot(Ping::init, "ping", args);
}
```

Apollo-standalone also provides features that simplify the:
- using http clients for sending requests to other services
- configuring logging?

The [StandaloneService#builder](src/main/java/com/spotify/apollo/standalone/StandaloneService.java)
method is a good summary of the modules you get in the bundle. You can find documentation for each
module in its respective [folder](../modules).

Minimal project skeleton
========================

### FIXME include ping in opensource-apollo?

The code examples are for a service called `ping`
([ghe:tools/ping](https://ghe.spotify.net/tools/ping)).

### TODO include opensource cookiecutter skeleton?
There's also a project template that you can use to spawn a new repository from:
([ghe:skeletons/simple-apollo-standalone](https://ghe.spotify.net/skeletons/simple-apollo-standalone)).


```plain
.
├── pom.xml
└── src/
    └── main/
        ├── java/
        │   └── com/
        │       └── example/
        │           └── Ping.java
        └── resources/
            └── ping.conf
```

### FIXME use apollo-bom

`./pom.xml`
```xml
<project>
    <modelVersion>4.0.0</modelVersion>

    <name>Simple Ping Service</name>
    <artifactId>ping</artifactId>
    <version>0.0.1-SNAPSHOT</version>

    <parent>
        <groupId>com.spotify</groupId>
        <artifactId>apollo-standalone</artifactId>
        <version>1.0.0</version>
    </parent>

    <properties>
        <apollo.runner>com.example.Ping</apollo.runner>
    </properties>
</project>
```

`./ping.conf`
```
# Configuration for http interface
http.server.port = 8080
http.server.port = ${?HTTP_PORT}
```

`./src/main/java/com/example/Ping.java`
```java
package com.example;

import com.spotify.apollo.AppInit;
import com.spotify.apollo.Environment;
import com.spotify.apollo.route.Route;
import com.spotify.apollo.standalone.LoadingException;
import com.spotify.apollo.standalone.StandaloneService;

public final class Ping {

  /**
   * The main entry point of the java process which will delegate to
   * {@link StandaloneService#boot(AppInit, String, String...)}.
   *
   * @param args  program arguments passed in from the command line
   * @throws LoadingException if anything goes wrong during the service boot sequence
   */
  public static void main(String[] args) throws LoadingException {
    StandaloneService.boot(Ping::init, "ping", args);
  }

  /**
   * An implementation of the {@link AppInit} functional interface which simply sets up a
   * "hello world" handler on the root route "/".
   *
   * @param environment  The Apollo {@link Environment} that the service is in.
   */
  static void init(Environment environment) {
    environment.routingEngine()
        .registerRoute(Route.sync("GET", "/ping", requestContext -> "pong"));
  }
}
```

Compile and Run
===============
TBW
