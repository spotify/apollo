# Apollo Metrics

This module integrates the [semantic-metrics](https://github.com/spotify/semantic-metrics) library
with the Apollo request/response handling facilities.

Including this this module in your assembly means that Apollo will add some metrics tracking
requests per endpoint.


## Usage

Maven dependency

```xml
<dependency>
    <groupId>com.spotify</groupId>
    <artifactId>apollo-metrics</artifactId>
    <version>1.0.0</version>
</dependency>
```

To include the metrics module, add it to your service assembly (e.g. the
[http-service](https://github.com/spotify/apollo/tree/master/apollo-http-service) assembly) by doing:

```java
public static void main(String[] args) throws LoadingException {
  final Service myService = HttpService.usingAppInit(MyService::init, "my-service")
      .withModule(MetricsModule.create())
      .build();

  HttpService.boot(myService, args);
}
```

Or if using [apollo-core](https://github.com/spotify/apollo/tree/master/apollo-core) directly:

```java
public static void main(String[] args) throws IOException, InterruptedException {
  final Service myService = Services.usingName("my-service")
      .withModule(ApolloEnvironmentModule.create())
      .withModule(MetricsModule.create())
      .build();

  try (Service.Instance instance = myService.start(args)) {
    // set up

    instance.waitForShutdown();
  }
}
```

Note that the metrics module requires [apollo-environment](https://github.com/spotify/apollo/tree/master/apollo-environment).


## Metrics

All metrics will be tagged with the following tags:

| tag         | value                      | comment                                              |
|-------------|----------------------------|------------------------------------------------------|
| service     | name of Apollo application |                                                      |
| endpoint    | method:uri-path-segment    | For instance, GET:/android/v1/search-artists/<query> |
| component   | "service-request"          |                                                      |

The following metrics are recorded:

### Endpoint request rate

A Meter metric, per status-code, tagged with:

| tag         | value                      | comment                                              |
|-------------|----------------------------|------------------------------------------------------|
| what        | "endpoint-request-rate"    |                                                      |
| status-code | *                          | "200", "404", "418", etc.                            |
| unit        | "request"                  |                                                      |

### Endpoint request duration

A Timer metric, tagged with:

| tag         | value                      | comment                                              |
|-------------|----------------------------|------------------------------------------------------|
| what        | "endpoint-request-duration"|                                                      |

### Fan-out

A Histogram metric, tagged with:

| tag         | value                      | comment                                              |
|-------------|----------------------------|------------------------------------------------------|
| what        | "request-fanout-factor"    |                                                      |
| unit        | "request/request"          |                                                      |

This metric will show you how many downstream requests each endpoint tends to make.


## Custom Metrics

To set up custom metrics on the application level you'll need to get hold of the
`SemanticMetricRegistry` object. Use:
```java
  environment.resolve(SemanticMetricRegistry.class);
```

You can then construct `MetricId`:s and emit metrics in handlers and middleware.

To tag custom metrics with endpoint information, you could pass in the endpoint name, or transform
whole routes. The example below shows both approaches.

Alternatively, you could set up an `EndpointRunnableFactoryDecorator` and register
it, similar to how the metrics module does:
```java
    Multibinder.newSetBinder(binder(), EndpointRunnableFactoryDecorator.class)
        .addBinding().to(MetricsCollectingEndpointRunnableFactoryDecorator.class);
```
See [Extending incoming/outgoing request handling]
(https://github.com/spotify/apollo/tree/master/apollo-environment#extending-incomingoutgoing-request-handling)
for some description of how to do request handling decorations.

For client-side metrics, wrap the ```Client``` you get from the ```RequestContext``` in a similar
way to how ```DecoratingClient``` is implemented. In your wrapper, ensure that the right metrics
are tracked.

### Custom per-endpoint response payload size histogram example

```java
  /**
   * Use this method to transform a route to one that tracks response payload sizes in a Histogram,
   * tagged with an endpoint tag set to method:uri of the route.
   */
  public Route<AsyncHandler<Response<ByteString>>> withResponsePayloadSizeHistogram(
      Route<AsyncHandler<Response<ByteString>>> route) {

    String endpointName = route.method() + ":" + route.uri();

    return route.withMiddleware(responsePayloadSizeHistogram(endpointName));
  }

  /**
   * Middleware to track response payload size in a Histogram,
   * tagged with an endpoint tag set to the given endpoint name.
   */
  public Middleware<AsyncHandler<Response<ByteString>>, AsyncHandler<Response<ByteString>>>
      responsePayloadSizeHistogram(String endpointName) {

    final MetricId histogramId = MetricId.build()
        .tagged("service", serviceName)
        .tagged("endpoint", endpointName)
        .tagged("what", "endpoint-response-size");

    final Histogram histogram = registry.histogram(histogramId);

    return (inner) -> (requestContext) ->
        inner.invoke(requestContext).whenComplete(
            (response, t) -> {
              if (response != null) {
                histogram.update(response.payload().map(ByteString::size).orElse(0));
              }
            }
        );
  }
```


## [ffwd](https://github.com/spotify/ffwd) reporter

The metrics module includes the [ffwd reporter](https://github.com/spotify/semantic-metrics#provided-plugins)
easily configurable from the apollo service configuration.

key | type | required | note
--- | --- | --- | ---
`ffwd.host` | string | optional | host of ffwd agent, default:`localhost`
`ffwd.port` | int | optional | port of ffwd agent, default:`19091`
`ffwd.interval` | int| optional | reporting interval in seconds, default:`30`
