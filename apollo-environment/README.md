# Apollo Environment

The `apollo-environment` module lets you create a fully functional
[`Environment`](../apollo-api/src/main/java/com/spotify/apollo/Environment.java) for an
[`AppInit`](../apollo-api/src/main/java/com/spotify/apollo/AppInit.java).

The environment will contain a `RoutingEngine` and a configurable, managed
[`Client`](../apollo-api/src/main/java/com/spotify/apollo/Client.java).

The main module is `ApolloEnvironmentModule` providing an `ApolloEnvironment` instance that can be
used to initialize an `AppInit` instance, returning a `RequestHandler` to be used with any
server module (for example the [`jetty-http-server`](../modules/jetty-http-server)).

## Configuration

key | type | required | note
--- | --- | --- | ---
`apollo.backend` | string | optional | eg., `example.org`
`apollo.logIncomingRequests` | boolean | optional | default `true`


## Example

```java
public static void main(String[] args) {
  final Service service = Services.usingName("ping")
      .withModule(HttpServerModule.create())
      .withModule(ApolloEnvironmentModule.create())
      .build();

  try (Service.Instance i = service.start(args)) {
    // Create the application (possible to get instances from i.resolve())
    final Application app = new App();

    // Create Environment and call App.init(env)
    final ApolloEnvironment env = ApolloEnvironmentModule.environment(i);
    final RequestHandler handler = env.initialize(app);

    // Create servers
    final HttpServer httpServer = HttpServerModule.server(i);

    // Servers will not be bound until these calls
    httpServer.start(handler);

    i.waitForShutdown();
  } catch (InterruptedException | IOException e) {
    // handle errors
  }
}
```

For a runnable example, see [`MinimalRunner`]
(../apollo-http-service/src/test/java/com/spotify/apollo/httpservice/MinimalRunner.java)


## Extending incoming/outgoing request handling

`ApolloEnvironmentModule` has a few extension points that allow 3rd party modules to
decorate internal components involved in incoming/outgoing request handling.


### IncomingRequestAwareClient
One important aspect of the Apollo `Client` is that it does not come with any protocol
support out of the box. Instead, support for different protocols should be added by
modules. These modules do so by injecting themselves into the `IncomingRequestAwareClient`
decoration chain.

The decoration chain looks like:

1. `OutgoingCallsGatheringClient`
1. `ServiceSettingClient`
1. `[IncomingRequestAwareClient]* <- Set<ClientDecorator>`
1. `NoopClient`


### RequestRunnableFactory

1. `[RequestRunnableFactory]*   <- Set<RequestRunnableFactoryDecorator>`
1. `RequestRunnableImpl`


### EndpointRunnableFactory

1. `TrackingEndpointRunnableFactory`
1. `[EndpointRunnableFactory]*  <- Set<EndpointRunnableFactoryDecorator>`
1. `EndpointInvocationHandler`


### RequestHandler
This is what is ultimately created from the `ApolloEnvironmentModule`. It will use
the `RequestRunnableFactory`, `EndpointRunnableFactory` and `IncomingRequestAwareClient`
decoration chains that were constructed above. See `RequestHandlerImpl` for how they are
used, but in short terms it's something like:

```java
RequestHandler requestHandler = ongoingRequest ->
    rrf.create(ongoingRequest).run((ongoingRequest, match) ->
        erf.create(ongoingRequest, requestContext, endpoint).run());
```


### Injecting decorators
To contribute to any of the sets of decorators mentioned above, use Guice Multibinder.

Here's an example of how a `ClientDecorator` is injected:

```java
@Override
protected void configure() {
  Multibinder.newSetBinder(binder(), ClientDecorator.class)
      .addBinding().toProvider(HttpClientDecoratorProvider.class);
}
```
