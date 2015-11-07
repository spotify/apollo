# Apollo API

This Apollo library gives you the tools you need to define your service routes and your
request/reply handlers. For some overview documentation, see:

* [AppInit & Environment](docs/app-init-environment.md)
* [Routing Engine](docs/routing-engine.md)
* [Routes](docs/routes.md)
* [Response](docs/response.md)
* [Middleware](docs/middleware.md)

## Just give me the bare minimum

### 1. `src/main/java/com/spotify/Small.java`

```java
package com.spotify;

import com.spotify.apollo.AppInit;
import com.spotify.apollo.Environment;
import com.spotify.apollo.route.Route;
import com.spotify.apollo.standalone.LoadingException;
import com.spotify.apollo.standalone.StandaloneService;

public final class Small {

  /**
   * The main entry point of the java process which will delegate to
   * {@link StandaloneService#boot(AppInit, String, String...)}.
   *
   * @param args  program arguments passed in from the command line
   * @throws LoadingException if anything goes wrong during the service boot sequence
   */
  public static void main(String[] args) throws LoadingException {
    StandaloneService.boot(Small::init, "small", args);
  }

  /**
   * An implementation of the {@link AppInit} functional interface which simply sets
   * up a "hello world" handler on the root route "/".
   *
   * @param environment  The Apollo {@link Environment} that the service is in.
   */
  static void init(Environment environment) {
    environment.routingEngine()
        .registerRoute(Route.sync("GET", "/", requestContext -> "hello world"));
  }
}
```

### 2. Build it with Maven!

TODO: link to a full template pom for a standalone service

Add a dependency to `apollo-standalone-service` to your `pom.xml`. Use a build property for the
version since you'll need it later in the build configuration.

```xml
<properties>
    <apollo.version>1.0.0-rc1</apollo.version>
</properties>
```

TODO: apollo-bom?
```xml
<dependency>
    <groupId>com.spotify</groupId>
    <artifactId>apollo-standalone-service</artifactId>
    <version>${apollo.version}</version>
</dependency>
```

Set up the build to produce a jar with a classpath pointing to the dependency jars under `lib/`

```xml
<build>
    <finalName>${project.artifactId}</finalName>
    <plugins>
        <plugin>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.3</version>
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
            <version>2.5</version>
            <configuration>
                <archive>
                    <addMavenDescriptor>true</addMavenDescriptor>
                    <manifest>
                        <addDefaultImplementationEntries>true</addDefaultImplementationEntries>
                        <addDefaultSpecificationEntries>true</addDefaultSpecificationEntries>
                        <addClasspath>true</addClasspath>
                        <classpathPrefix>lib/</classpathPrefix>
                        <mainClass>com.spotify.Small</mainClass>
                    </manifest>
                    <manifestEntries>
                        <X-Spotify-Apollo-Version>${apollo.version}</X-Spotify-Apollo-Version>
                    </manifestEntries>
                </archive>
            </configuration>
        </plugin>
    </plugins>
</build>
```

### 3. Build & Run it!

```
mvn package
java -jar target/small.jar -Dhttp.server.port=8080
```

### 4. Curl it!

```
curl http://localhost:8080/
> hello world
```
