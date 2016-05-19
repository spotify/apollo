Apollo Test
===========

This is a library containing tools for testing your Apollo applications.

Include the maven dependency in your project pom.xml
```xml
<dependency>
  <groupId>com.spotify</groupId>
  <artifactId>apollo-test</artifactId>
  <version>${apollo.version}</version>  <!-- (version not needed with apollo-bom) -->
  <scope>test</scope>
</dependency>
```
If you get MethodNotFound exceptions from Hamcrest move this dependency to the top. E.g. mockito and junit
adds their own (older) versions of hamcrest to the classpath.


# Introduction

When testing your apollo service code there are some things that need some extra attention. Things 
like the fact that your code uses the `RequestContext.requestScopedClient()` to make service calls
and that the whole request handler returns a `CompletionStage`. You'll have to mock the
`RequestContext` instance and set up interaction rules for how each request should be identified 
and what the response should be. Furthermore you should have actual asynchronous responses 
instead of just returning a `CompletableFuture.completedFuture(T)` if you want to test how the
transformations actually compose when responses come in after the transformations are set up in
the handler.

This library aims to help with testing your services with a few helpers: 
[`ServiceHelper`](src/main/java/com/spotify/apollo/test/ServiceHelper.java) and 
[`StubClient`](src/main/java/com/spotify/apollo/test/StubClient.java).


### We'll start with a minimal example:

our application under test
```java
class MinimalApp {

  static void main(String... args) throws LoadingException {
    HttpService.boot(MinimalApp::init, "test", args);
  }

  static void init(Environment environment) {
    environment.routingEngine()
        .registerAutoRoute(Route.future("GET", "/beer", (ctx) -> beer(ctx)));
  }

  static CompletionStage<Response<String>> beer(RequestContext context) {
    Request breweryRequest = Request.forUri("http://brewery/order");
    CompletionStage<Response<ByteString>> orderRequest = context.requestScopedClient()
        .send(breweryRequest);

    return orderRequest.thenApply(orderResponse -> {
      if (orderResponse.status().code() != Status.OK.code()) {
        return Response.forStatus(Status.INTERNAL_SERVER_ERROR);
      }

      // assume we get an order id as a plaintext payload
      final String orderId = orderResponse.payload().get().utf8();

      return Response.forPayload("your order is " + orderId);
    });
  }
}
```

So we have a simple endpoint at `http://<app>/beer` that makes a GET call to the `brewery` service
for an order (yes this isn't a nice API but thankfully it's imaginary). Then it transforms the
response into a simple string response telling us what our order id is. It also checks the response
code of the `brewery` call to make sure we got a `200 OK` back. If not, it will reply with a `500
Internal Server Error`.

### Let's write some tests

An example how tests that call this endpoint and mock the `brewery` calls would be written using the 
test library.

```java
import static com.spotify.apollo.test.unit.ResponseMatchers.hasStatus;
import static com.spotify.apollo.test.unit.StatusTypeMatchers.withCode;

public class MinimalAppTest {

  private static final String BREWERY_ORDER_URI = "http://brewery/order";
  private static final String ORDER_REPLY = "order0443";
  private static final ByteString ORDER_REPLY_BYTES = ByteString.encodeUtf8(ORDER_REPLY);

  @Rule
  public ServiceHelper serviceHelper = ServiceHelper.create(MinimalApp::init, "test");

  public StubClient stubClient= serviceHelper.stubClient();

  @Test
  public void shouldRespondWithOrderId() throws Exception {
    stubClient.respond(Response.forPayload(ORDER_REPLY_BYTES)).to(BREWERY_ORDER_URI);

    CompletionStage<Response<ByteString>> replyFuture = serviceHelper.request("GET", "/beer");
    String reply = replyFuture.toCompletableFuture().get().payload().get().utf8();

    assertThat(reply, is("your order is " + ORDER_REPLY));
  }

  @Test
  public void shouldFailForBadStatusCode() throws Exception {
    stubClient.respond(Response.of(StatusCode.IM_A_TEAPOT, ORDER_REPLY_BYTES).to(BREWERY_ORDER_URI);

    CompletionStage<Response<ByteString>> replyFuture = serviceHelper.request("GET", "/beer");

    Response<ByteString> response = replyFuture.toCompletableFuture().get();
    assertThat(response, hasStatus(withCode(Status.INTERNAL_SERVER_ERROR)));
  }
}
```

Here we have a unit test that creates a `ServiceHelper` that will use `MinimalApp::init` to create
our routes. From the helper we can get a `StubClient` for setting up replies to outgoing requests.
The actual tests then should be pretty self-explanatory. They simply use `stubClient.respond(...)`
and its overloads to setup different request-reply scenarios. Then they make the call to our
endpoint using `serviceHelper.send(...)` and verify the response.

### Hamcrest Matcher Utilities

There are a couple of handy utility classes that can help you write your tests more succinctly:

* [RequestMatchers.java](src/main/java/com/spotify/apollo/test/unit/RequestMatchers.java)
* [ResponseMatchers.java](src/main/java/com/spotify/apollo/test/unit/ResponseMatchers.java)
* [RouteMatchers.java](src/main/java/com/spotify/apollo/unit/RouteMatchers.java)
* [StatusTypeMatchers.java](src/main/java/com/spotify/apollo/unit/StatusTypeMatchers.java)

#### More tests

* [ServiceHelperTest.java](src/test/java/com/spotify/apollo/test/helper/ServiceHelperTest.java)
* [StubClientTest.java](src/test/java/com/spotify/apollo/test/StubClientTest.java)
