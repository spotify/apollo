Apollo HTTP Service
===================

[![Maven Central](https://img.shields.io/maven-central/v/com.spotify/apollo-parent.svg)](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.spotify%22%20apollo*)

The `apollo-http-service` library is a small bundle of Apollo modules. It incorporates both
[apollo-api](../apollo-api) and [apollo-core](../apollo-core) and ties them together with the
[jetty-http-server](../modules/jetty-http-server) and the [okhttp-client](../modules/okhttp-client)
to make a complete service. It also adds `logback-classic` as an SLF4J
implementation to give you logging capabilities.

Apollo HTTP Service gives you what you need to start your backend service. Here, for example, we
tell `HttpService` to boot a service named `"ping"`, defined by the function `Ping::init`, and
handle any command line arguments passed in through the `args` variable:

```java
public static void main(String... args) throws LoadingException {
  HttpService.boot(Ping::init, "ping", args);
}
```

The [HttpService](src/main/java/com/spotify/apollo/httpservice/HttpService.java)
class is a good example of how `apollo-core` together with `apollo-api` and other modules come
together to build a fully functional service. You can find documentation for each
module in its respective directory under [/modules](../modules).

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
  public static void main(String... args) throws LoadingException {
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

`./src/main/resources/ping.conf`
```
# Configuration for http interface
http.server.port = 8080
http.server.port = ${?HTTP_PORT}
```

For more information on how to manage configuration, see [Apollo Core](../apollo-core),
the [logback-classic](http://logback.qos.ch/) and the [Typesafe Config](https://github.com/typesafehub/config) documentation.

### Maven

`./pom.xml`
```xml
<project>
    <modelVersion>4.0.0</modelVersion>

    <name>Simple Ping Service</name>
    <groupId>com.example</groupId>
    <artifactId>ping</artifactId>
    <version>0.0.1-SNAPSHOT</version>

    <properties>
        <mainClass>com.example.Ping</mainClass>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>com.spotify</groupId>
                <artifactId>apollo-bom</artifactId>
                <version>1.1.0</version>
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

   <build>
       <finalName>${project.artifactId}</finalName>
        <plugins>
            <plugin>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.1</version>
                <configuration>
                    <source>1.8</source>
                    <target>1.8</target>
                    <compilerArgs>
                        <compilerArg>-Xlint:all</compilerArg>
                    </compilerArgs>
                </configuration>
            </plugin>
            <plugin>
                <artifactId>maven-dependency-plugin</artifactId>
                <version>2.10</version>
                <executions>
                    <execution>
                        <phase>prepare-package</phase>
                        <goals>
                            <goal>copy-dependencies</goal>
                        </goals>
                    </execution>
                </executions>
                <configuration>
                    <useBaseVersion>false</useBaseVersion>
                    <overWriteReleases>false</overWriteReleases>
                    <overWriteSnapshots>true</overWriteSnapshots>
                    <includeScope>runtime</includeScope>
                    <outputDirectory>${project.build.directory}/lib</outputDirectory>
                </configuration>
            </plugin>
            <plugin>
                <artifactId>maven-jar-plugin</artifactId>
                <version>2.6</version>
                <configuration>
                    <archive>
                        <manifest>
                          <addClasspath>true</addClasspath>
                          <classpathPrefix>lib/</classpathPrefix>
                          <mainClass>${mainClass}</mainClass>
                        </manifest>
                    </archive>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

The Maven configuration is a bit lengthy so it deserves some more explanation:

* We use a property named `mainClass` to refer to our main class. This will be used later.
* Under the `dependencyManagement` we import all the Apollo artifact versions through the `apollo-bom` artifact. For more information about importing managed dependencies, see the [Maven documentation](https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html#Importing_Dependencies).
* We configure the compiler plugin to target JDK 8.
* We configure the `maven-dependency-plugin` to copy all runtime dependency jars into `${project.build.directory}/lib`. These will be referenced from the main artifact.
* We configure `maven-jar-plugin` to add the classpath jars to the manifest, prefixed with `lib/` along with the `MainClass` entry to use our main class.

Compile and Run
===============
```
mvn package
java -jar target/ping.jar
```

Try a request with `curl`
```
$ curl http://localhost:8080/ping
pong
```
