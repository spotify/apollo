[![Circle Status](https://circleci.com/gh/spotify/apollo.svg?style=shield&circle-token=5a9eb086ae3cec87e62fc8b6cdeb783cb318e3b9)](https://circleci.com/gh/spotify/apollo)
[![Maven Central](https://img.shields.io/maven-central/v/com.spotify/apollo-parent.svg)](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.spotify%22%20apollo*)

Apollo
======

Apollo is a set of Java libraries that we use at Spotify when writing microservices. Apollo
includes an internal http server, making it easy to implement a restful API service. There are
three main libraries in Apollo:

* [apollo-http-service](apollo-http-service)
* [apollo-api](apollo-api)
* [apollo-core](apollo-core)

### Apollo HTTP Service
The [apollo-http-service](apollo-http-service) library is a standardized assembly of Apollo
modules. It incorporates both apollo-api and apollo-core and ties them together with other
modules to get a standard api service using http for incoming and outgoing communication.

### Apollo API
The [apollo-api](apollo-api) library is the Apollo library you are most likely to interact with.
It gives you the tools you need to define your service routes and your request/reply handlers.

Here, for example, we define that our service will respond to a GET request on the path `/` with
the string `"hello world"`:
```java
public static void init(Environment environment) {
  environment.routingEngine()
      .registerRoute(Route.sync("GET", "/", requestContext -> "hello world"));
}
```

> Note that, for an Apollo-based service, you can see the routes defined for a service by querying
[`/_meta/0/endpoints`](apollo-api-impl/src/main/java/com/spotify/apollo/meta/model).

The apollo-api library provides several ways to help you define your request/reply handlers.
You can specify how responses should be serialized (such as JSON). Read more about
this library in the [Apollo API Readme](apollo-api).

### Apollo Core
The [apollo-core](apollo-core) library manages the lifecycle (loading, starting, and stopping) of
your service. You do not usually need to interact directly with apollo-core; think of it merely 
as "plumbing". For more information about this library, see the [Apollo Core Readme](apollo-core).

### Apollo Test
In addition to the three main Apollo libraries listed above, to help you write tests for your
service we have an additional library called [apollo-test](apollo-test). It has helpers to set up
a service for testing, and to mock outgoing request responses.

### Apollo Releases
For releases see the [releases](https://github.com/spotify/apollo/releases) page.

### Getting Started with Apollo
The quickest way to get started with Apollo is by setting up an
[apollo-http-service](apollo-http-service), and run it directly (`java -jar ...`)
(or in a docker container).

This is a complete service:
```java
public class MiniService {
  public static void main(String[] args) throws LoadingException {
    StandaloneService.boot(
        env ->
            env.routingEngine().registerRoute(
                Route.sync("GET", "/hello", context -> "Hello Apollo")),
        "mini-service", args);
  }
}
```
