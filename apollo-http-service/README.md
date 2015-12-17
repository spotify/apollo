Apollo HTTP Service
===================

The `apollo-http-service` library is a small bundle of Apollo modules. It incorporates both
[apollo-api](../apollo-api) and [apollo-core](../apollo-core) and ties them together with a http
server and client to make a complete service.

Apollo HTTP Service gives you what you need to start your backend service. Here, for example, we
tell `HttpService` to boot a service named `"ping"`, defined by the function `Ping::init`, and
handle any command line arguments passed in through the `args` variable:

```java
public static void main(String[] args) throws LoadingException {
  HttpService.boot(Ping::init, "ping", args);
}
```

The [HttpService#builder](src/main/java/com/spotify/apollo/httpservice/HttpService.java)
method is a good summary of the modules you get in the bundle. You can find documentation for each
module in its respective [folder](../modules).

Minimal project skeleton
========================

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

`./pom.xml`
```xml
<project>
    <modelVersion>4.0.0</modelVersion>

    <name>Simple Ping Service</name>
    <groupId>com.example</groupId>
    <artifactId>ping</artifactId>
    <version>0.0.1-SNAPSHOT</version>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>com.spotify</groupId>
                <artifactId>apollo-bom</artifactId>
                <version>1.0.0</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

   <dependencies>
        <dependency>
            <groupId>com.spotify</groupId>
            <artifactId>apollo-http-service</artifactId>
        </dependency>
   </dependencies>

   <!-- TODO: build runnable jar -->
</project>
```

`./src/main/resources/ping.conf`
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
import com.spotify.apollo.httpservice.LoadingException;
import com.spotify.apollo.httpservice.HttpService;

public final class Ping {

  /**
   * The main entry point of the java process which will delegate to
   * {@link HttpService#boot(AppInit, String, String...)}.
   *
   * @param args  program arguments passed in from the command line
   * @throws LoadingException if anything goes wrong during the service boot sequence
   */
  public static void main(String[] args) throws LoadingException {
    HttpService.boot(Ping::init, "ping", args);
  }

  /**
   * An implementation of the {@link AppInit} functional interface which simply sets up a
   * "hello world" handler on the root route "/".
   *
   * @param environment  The Apollo {@link Environment} that the service is in.
   */
  static void init(Environment environment) {
    environment.routingEngine()
        .registerAutoRoute(Route.sync("GET", "/ping", requestContext -> "pong"));
  }
}
```

Compile and Run
===============
TODO: TBW
```
mvn package
java -jar target/app.jar -Dhttp.server.port=8080
```
