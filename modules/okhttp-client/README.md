# HTTP Client

The `okhttp-client` module provides a `HttpClient` that can be used to make
HTTP requests which is backed by the [okhttp](https://github.com/square/okhttp)
library.

This module uses version 2.5.0 of okhttp.

## Configuration

If a configuration property is not set, the default from okhttp is used.

key | type | required | note
--- | ---- | -------- | ----
`http.client.connectTimeout` | int | optional | milliseconds; defaults to 10000
`http.client.readTimeout` | int | optional | milliseconds; defaults to 10000
`http.client.writeTimeout` | int | optional | milliseconds; defaults to 10000
`http.client.maxIdleConnections` | int | optional | maximum number of idle connections to keep in the connection pool; defaults to system property `http.maxConnections` if set, else 5
`http.client.keepAliveDuration` | int | optional | Time in milliseconds to keep the connection alive in the pool before closing it; defaults to system property `http.keepAliveDuration` if set, else 5 minutes
`http.client.followRedirects` | boolean | optional | Explicitly enable or disable following http redirect responses, default is behavior is to follow redirects (true)
`http.client.async.maxRequests` | int | optional | maximum number of asynchronous requests to execute concurrently; defaults to 64. Above this requests queue in memory, waiting for the running calls to complete.
`http.client.async.maxRequestsPerHost` | int | optional | Set the maximum number of requests for each host to execute concurrently. This limits requests by the URL's host name. Defaults to 5

## Example

```java
public static void main(String[] args) throws Exception {

  Service service = Services.usingName("test")
      .withModule(HttpClientModule.create())
      .withModule(ApolloEnvironmentModule.create())
      .build();

  try (Service.Instance instance = service.start(args)) {
    Client httpClient = instance.resolve(ApolloEnvironment.class).environment().client();
    RequestHandler handler = new ProxyHandler(httpClient);
    HttpServer httpServer = HttpServerModule.server(instance);

    httpServer.start(handler);
    instance.waitForShutdown();
  }
}

/** Proxies requests to another service */
public static class ProxyHandler implements RequestHandler {
  private final Client httpClient;

  public Handler(Client httpClient) {
    this.httpClient = httpClient;
  }

  public void handle(OngoingRequest originalRequest) {
    Request req = Request.forUri("http://proxy.example");
    httpClient.send(req, Optional.of(originalRequest))
      .thenAccept(response -> originalRequest.reply(response));
  }
}
```
## Adding metrics to outgoing http requests

In order to have metrics for the outgoing http requests, the HttpMetricModule should be used
 together with HttpClientModule. 

```java
public static void main(String[] args) throws Exception {

   final Service service =
          Services.usingName(SERVICE_NAME)
              .withModule(StandaloneModule.create(Main::configure))
              .withEnvVarPrefix("SPOTIFY")
              .withModule(HttpClientModule.create())
              .withModule(HttpMetricModule.create())
              .build();
}
```

It's important that `HttpMetricModule` to be declared **AFTER** `HttpClientModule`, otherwise the decorator won't work as expected. This is a short-coming of the framework that we [plan](https://github.com/spotify/apollo/issues/362) to fix in the future.
