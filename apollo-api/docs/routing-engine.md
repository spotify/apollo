# Routing Engine

[`RoutingEngine`](/apollo-api/src/main/java/com/spotify/apollo/Environment.java#L72) is part of
[`Environment`](/apollo-api/src/main/java/com/spotify/apollo/Environment.java) and it's used to
register [`Route`](/apollo-api/src/main/java/com/spotify/apollo/route/Route.java) or
[`RouteProvider`](/apollo-api/src/main/java/com/spotify/apollo/route/RouteProvider.java) instances.

## Example

```java
void init(Environment environment) {
  environment.routingEngine()
      .registerRoute(Route.sync("GET", "/ping", requestContext -> "pong"))
      .registerRoutes(new MyResource());
}
```

The implementation of `MyResource` as a class by itself.

```java
class MyResource implements RouteProvider {

  @Override
  public Stream<? extends Route<? extends AsyncHandler<?>>> routes() {
    return Stream.of(
        Route.sync("GET", "/v1/address/<name>", requestContext -> "!"/* do work */),
        Route.sync("PUT", "/v1/address/<name>", requestContext -> "!"/* do work */)
    );
  }
}
```

## Overlapping route paths

Say you have defined two routes that overlap in a way that makes one of them a specific case of the
other more general one. An example of such an overlap is:

1. `/foo/bar`
1. `/foo/<arg>`

Route 2 can match calls to `/foo/bar` too, thus making route 1 a special case of route 2. In this
case, the `RoutingEngine` will route to the more specific routes before more general ones.
