# Going from Classes to Lambdas with Java 8

Java 8 blurs the line between Object Oriented Programming and Functional Programming. This page
aims to make it a bit easier to understand how interfaces, objects and function lambdas interact.

We'll use a running example using Middlewares. We start with an OO implementation and gradually
rewrite it to one using lambdas and method references.

---

In this example we define a route that has a handler, `EndpointHandler`, and a Middleware that
decorates the handler using `CacheHeaderHandler` which adds cache headers to the response of the
inner handler.

> Note that the Middleware decorate a `SyncHandler<String>` into a `SyncHandler<Response<String>>`
> so that it can add the extra cache control headers.

Here's the mostly OO implementation:

```java
class ExampleApp implements AppInit {

  @Override
  public void create(Environment environment) {
    // the use of Middleware::syncToAsync is necessary since registerAutoRoute expects a
    // Route of an AsyncHandler<T> while all of our handlers are SyncHandler<T>

    environment.routingEngine()
        .registerAutoRoute(
            Route.create("GET", "/ping", new EndpointHandler())
                .withMiddleware(new CacheHeaderDecorator())
                .withMiddleware(Middleware::syncToAsync));
  }

  static class EndpointHandler implements SyncHandler<String> {

    @Override
    public String invoke(RequestContext requestContext) {
      return "pong";
    }
  }

  static class CacheHeaderDecorator implements Middleware<SyncHandler<String>, SyncHandler<Response<String>>> {

    @Override
    public SyncHandler<Response<String>> apply(SyncHandler<String> innerHandler) {
      return new CacheHeaderHandler(innerHandler);
    }
  }

  static class CacheHeaderHandler implements SyncHandler<Response<String>> {

    private final SyncHandler<String> innerHandler;

    CacheHeaderHandler(SyncHandler<String> innerHandler) {
      this.innerHandler = innerHandler;
    }

    @Override
    public Response<String> invoke(RequestContext requestContext) {
      String response = innerHandler.invoke(requestContext);
      return Response.forPayload(response)
          .withHeader("cache-control", "private")
          .withHeader("max-age", "0");
    }
  }
}
```

We know that `SyncHandler` and `Middleware` both are functional interfaces (interfaces with only
one method) so let's replace them by lambdas and method references.

First `EndpointHandler` is just a class containing one method. It can be a regular method that we
call from an inline lambda in the route definition:

```java
class ExampleApp implements AppInit {

  @Override
  public void create(Environment environment) {
    // the use of SyncHandler<String> is to help java with the handler type since it can't fully
    // infer it in this situation

    environment.routingEngine()
        .registerAutoRoute(
            Route.<SyncHandler<String>>create("GET", "/ping", requestContext -> "pong")
                .withMiddleware(new CacheHeaderDecorator())
                .withMiddleware(Middleware::syncToAsync));
  }

  static class CacheHeaderDecorator ...
  static class CacheHeaderHandler ...
}
```

Then we can see that `CacheHeaderDecorator` is simply a class with one function that calls the
constructor of `CacheHeaderHandler` with the inner handler. This can be replaced by a constructor
method reference and we can remove `CacheHeaderDecorator` all together (a so-called
[η-conversion](https://en.wikipedia.org/wiki/Lambda_calculus#.CE.B7-conversion)):

```java
class ExampleApp implements AppInit {

  @Override
  public void create(Environment environment) {
    environment.routingEngine()
        .registerAutoRoute(
            Route.<SyncHandler<String>>create("GET", "/ping", requestContext -> "pong")
                .withMiddleware(CacheHeaderHandler::new)
                .withMiddleware(Middleware::syncToAsync));
  }

  static class CacheHeaderHandler ...
}
```

The only class left now is `CacheHeaderHandler`. By closer inspection the only reason it is a
class is because it has a field holding a reference to the inner handler that it decorates. This
can be turned into a method that takes the inner handler as an argument and returns a handler
implemented inline using a lambda. Then we'll be able to get rid of all classes. Finally the
reference to `CacheHeaderHandler::new` will be replaced with a reference to this method which
will be the final implementation of the middleware.

```java
SyncHandler<Response<String>> cacheMiddleware(SyncHandler<String> innerHandler) {
  return requestContext -> {
    String response = innerHandler.invoke(requestContext);
    return Response.forPayload(response)
        .withHeader("cache-control", "private")
        .withHeader("max-age", "0");
  };
}
```

The final example will then be much cleaner and less bloated with static inner classes:

```java
class ExampleApp implements AppInit {

  @Override
  public void create(Environment environment) {
    environment.routingEngine()
        .registerAutoRoute(
            Route.<SyncHandler<String>>create("GET", "/ping", requestContext -> "pong")
                .withMiddleware(this::cacheMiddleware)
                .withMiddleware(Middleware::syncToAsync));
  }

  SyncHandler<Response<String>> cacheMiddleware(SyncHandler<String> innerHandler) {
    return requestContext -> {
      String response = innerHandler.invoke(requestContext);
      return Response.forPayload(response)
          .withHeader("cache-control", "private")
          .withHeader("max-age", "0");
    };
  }
}
```

We turned three classes implementing `SyncHandler` and `Middleware` into plain java functions and
lambdas without changing any of the semantics. This should give you a better intuition for how
common object oriented patterns with classes and decorators can be implemented using functional
concepts like lambdas and method references.
