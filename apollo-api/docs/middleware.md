# Middleware

> This section talks a lot about route handlers, so make sure to read the [Routes](/apollo-api/docs/routes.md) doc first.
>
> It also makes heavy use of Java 8 lambdas. To get a bit more familiar with lambdas and how they
> relate to object oriented programming in this setting, read the [Classes to Lambdas README](/apollo-api/docs/class-to-lambda.md).

Middlewares are functions that can be used to decorate the behavior of a route handler. This
can be used to add common functionality to several routes avoiding code duplication.

Because they are just functions that decorate an _inner_ handler, they provide a versatile tool for
implementing many classes of common functionality. These are some of the basic patterns that can
be implemented with Middleware together with some example uses.

A middleware can:

* Do something and then continue by transparently calling the inner handler
 * Logging requests
 * Collecting statistics
 * Darkloading other code paths
* Decide not to call the inner handler and instead respond to the request directly
 * ACL checks
 * Request validation
* After calling the inner handler, modify the response before replying
 * Add caching headers
 * Selecting response representation of a payload based on request headers
* Call the inner handler after modifying the request context in some way
 * Modifying the incoming request for compatibility reasons during code migration
 * Modifying the behavior of the request scoped client
* Call the inner handler multiple times
 * Retrying in case of recoverable failures

## Generic Example

This is how a middleware can be implemented. The comments outline how the different patterns
mentioned above can be implemented and how they interact with the `requestContext` and the
`innerHandler`.

```java
static <T> SyncHandler<Response<T>> myMiddleware(SyncHandler<T> innerHandler) {
  return requestContext -> {
    // Check condition before continuing dispatch
    final boolean isOkToProceed = condition(requestContext);

    if (!isOkToProceed) {
      return Response.forStatus(Status.UNAUTHORIZED);
    }

    LOGGER.info("Handling request to {}", requestContext.request().uri());

    // Decorate RequestContext
    final RequestContext loggingOutgoingCallsContext = loggingContext(requestContext);

    // Call inner handler
    final T innerResponse = innerHandler.invoke(loggingOutgoingCallsContext);

    return Response.forPayload(innerResponse)
        .withHeader("X-Added-Header", "With Some Value");
  };
}
```

This middleware can then be used together with route definitions.

```java
static void init(Environment environment) {
  environment.routingEngine()
      .registerAutoRoute(
          Route.<SyncHandler<String>>create("GET", "/foo", requestContext -> "hello world")
              .withMiddleware(Small::myMiddleware)
              .withMiddleware(Middleware::syncToAsync))
  ;
}
```

_Note the we need to use `Route.<SyncHandler<String>>create()` to obtain a `SyncHandler<T>` which is
the handler type our middleware is defined to work on. In the end we apply the
`Middleware::syncToAsync` middleware (defined in apollo-api) which turns the route handler into an
`AsyncHandler<T>` which the framework can call._

### Decorating a RequestContext

If you need to decorate a request before passing invoking your inner handlers, you will need to
create a new decorated `RequestContext`. This is how you can decorate a request with a custom
header:

```java
private RequestContext decorateRequestContext(RequestContext originalRequestContext) {
  Map<String, String> decoratedRequestHeaders = originalRequestContext.request().headers();
  decoratedRequestHeaders.put("Custom-Header", "Custom value");

  Request decoratedRequest = originalRequestContext.request()
      .withHeaders(decoratedRequestHeaders);

  return RequestContexts
      .create(decoratedRequest,
              originalRequestContext.requestScopedClient(),
              originalRequestContext.pathArgs(),
              originalRequestContext.metadata().arrivalTime().getNano(),
              originalRequestContext.metadata());
}
```

## Advanced: Creating custom context types

In our application, we'll have a mechanism for paging and authenticating requests. We'll model it
with these context types:

```java
interface AuthContext {
  Optional<String> user();
}

interface PagingContext {
  int page();
}
```

Two simple interfaces containing the information for an authenticated and a paged request. Note that
so far the two contexts have nothing to do with each other. We'll get to how they will be used
together.

The implementation of how to create these contexts will just be plain functions:

```java
PagingContext page(RequestContext c) {
  int page = c.request().getParameter("page")
      .map(Integer::parseInt)
      .orElse(0);

  return () -> page;
}

AuthContext auth(RequestContext c) {
  String userName = getUsername(c);

  return () -> Optional.ofNullable(userName);
}
```

So far so good. These are very straight forward. Just plain Java and Apollo is not even involved
yet.

Next, we'll turn to an endpoint that uses both contexts to produce a plain `String` response.

```java
String whereAmI(AuthContext authContext, PagingContext pagingContext) {
  return authContext.user() + ", you're on page " + pagingContext.page();
}
```

So how would we bind this endpoint to a `Route` that will have both an `AuthContext` and a
`PagingContext` available to it? Preferably we would like something as simple as this:

```java
Route.create("GET", "/test", authContext -> pageContext -> whereAmI(authContext, pageContext));
```

This almost works. `Route.create(...)` can actually take any type `T` as its handler. But Apollo
wouldn't know what to do with that handler unless it's an implementation of `AsyncHandler<T>`.
In this case (with a little help for the types) the handler would be a
`Function<AuthContext, Function<PagingContext, String>>`. We only need to tell
Apollo how to convert that into an `AsyncHandler<String>`.

To do this, we first declare two functional interfaces:

```java
interface Authenticated<T> extends Function<AuthContext, T> {}
interface Paged<T> extends Function<PagingContext, T> {}
```

These enable us to give a better name to `Function<AuthContext, Function<PagingContext, String>>`,
namely `Authenticated<Paged<T>>`.

Then we create a `Middleware` that converts between our two handler types. It will use both
`auth(RequestContext)` and `page(RequestContext)`.

```java
<T> Middleware<Authenticated<Paged<T>>, AsyncHandler<T>> authPaged() {
  return ap -> (requestContext) -> {
    T payload = ap
        .apply(auth(requestContext))
        .apply(page(requestContext));
    return immediateFuture(forPayload(payload));
  };
}
```

One can create one of these for any combination of contexts your application needs, or just one
with a bigger type that will contain all of the contexts all together. The important thing is that
it will be re-used to define many `Route`s.

Finally we can define `Route`s using the factory method which will help infer the types of the
handler lambda and make the whole thing look as nice as this:

```java
Route<AsyncHandler<String>> route =
    Route.with(authPaged(), "GET", "/test", auth -> page -> whereAmI(auth, page));
```

Mounting this with the `RoutingEngine` and making some requests to it, we'll get:

```
$ curl 'http://localhost:8080/ping/test'
Optional.empty, you're on page 0

$ curl 'http://rouz@localhost:8080/ping/test?page=4'
Optional[rouz], you're on page 4
```
