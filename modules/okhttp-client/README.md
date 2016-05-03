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
`http.client.async.maxRequests` | int | optional | maximum number of asynchronous requests to execute concurrently; defaults to 64. Above this requests queue in memory, waiting for the running calls to complete.
`http.client.async.maxRequestsPerHost` | int | optional | Set the maximum number of requests for each host to execute concurrently. This limits requests by the URL's host name. Defaults to 5

## Example

```java
public static void main(String[] args) throws Exception {
  Service service = Services.usingName("test")
      .withModule(HttpClientModule.create())
      .build();

  try (Service.Instance instance = service.start(args)) {
    HttpClient httpClient = instance.resolve(HttpClient.class);
    RequestHandler handler = new ProxyHandler(httpClient);
    HttpServer httpServer = HttpServerModule.server(instance);

    httpServer.start(handler);
    instance.waitForShutdown();
  }
}

/** Proxies requests to another service */
public static class ProxyHandler implements RequestHandler {
  private final HttpClient httpClient;

  public Handler(HttpClient httpClient) {
    this.httpClient = httpClient;
  }

  public void handle(OngoingRequest originalRequest) {
    Request req = Request.forUri("http://proxy.example");
    httpClient.send(req, Optional.of(originalRequest))
      .thenAccept(response -> originalRequest.reply(response));
  }
}
```
