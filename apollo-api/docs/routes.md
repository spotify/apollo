# Routes

There are basically two types of routes based on the type of request handler, synchronous and
asynchronous. Each route defines basic information like method, uri, documentation string and
handler.

The two main methods for creating routes are:

* `Route.sync(String method, String uri, SyncHandler<T> handler)`
* `Route.async(String method, String uri, AsyncHandler<T> handler)`

Since `RoutingEngine` expects routes of the type `Route<AsyncHandler<T>>`, both `Route.sync` and
`Route.async` return a `Route<AsyncHandler<T>>` and can thus be directly used for registering
routes.

A route added with `RoutingEngine.registerAutoRoute(s)()`, or using the `Middlewares::autoSerialize`
or `Middlewares::apolloDefaults` will serialize its response payload with the `AutoSerializer`.
Routes added with `RoutingEngine.registerRoutes()` must return `Response<ByteString>`s, and
no further processing will be done.

## Route handler reply types

Instead of returning a plain type `T`, a route handler may return a
[`Response<T>`](/apollo-api/src/main/java/com/spotify/apollo/Response.java), which is a wrapper
where you can specify extra information about the reply (see
[Response](/apollo-api/docs/response.md)).

The matrix of sync/async and plain/response combinations looks like this:

|      Type        | `SyncHandler<T>` | `AsyncHandler<T>` |
|:---------------: | -------------- | --------------- |
|     **`T`**      | `T` - A plain synchronous payload replied with status code `200 OK` | `CompletionStage<T>` - A plain asynchronous payload replied with status code `200 OK` |
| **`Response<T>`** | `Response<T>` - A synchronous payload with custom status code and headers | `CompletionStage<Response<T>>` - An asynchronous payload with custom status code and headers |

## Route providers

[`RouteProvider`](/apollo-api/src/main/java/com/spotify/apollo/route/RouteProvider.java) is a
functional interface for creating a
[`Stream`](https://docs.oracle.com/javase/8/docs/api/java/util/stream/package-summary.html)
of routes.

This is useful for when the application needs to group routes in some logical way and treat them
all in bulk. A REST-ful api with endpoints grouped by resource is a common use case.

```java
static class BlogPost implements RouteProvider {

  @Override
  public Stream<? extends Route<? extends AsyncHandler<?>>> routes() {
    return Stream.of(
        Route.sync("GET", "/blogpost/<id>", ctx -> getPost(ctx.pathArgs().get("id"))),
        Route.sync("POST", "/blogpost", ctx -> createPost(ctx))
    );
  }

  // Post getPost(String id) { ... }
  // Post createPost(RequestContext context) { ... }
}

public static void init(Environment environment) {
  // ...
  environment.routingEngine()
      .registerAutoRoutes(new BlogPost());
}
```

For more utilities that manipulate routes, see the [`apollo-extra`](/apollo-extra) module.
