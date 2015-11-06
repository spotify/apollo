package com.spotify.apollo.route;

import com.spotify.apollo.Client;
import com.spotify.apollo.Request;
import com.spotify.apollo.RequestContext;
import com.spotify.apollo.Response;
import com.spotify.apollo.request.RequestContexts;
import com.spotify.apollo.route.Route.DocString;

import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.Optional;

import autovalue.shaded.com.google.common.common.collect.ImmutableMap;
import okio.ByteString;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RouteEndpointTest {
  RouteEndpoint endpoint;

  Request request;
  RequestContext requestContext;
  Map<String, String> pathArgs;

  SyncHandler<Response<ByteString>> syncEndpointHandler;
  AsyncHandler<Response<ByteString>> asyncHandler;
  Response<ByteString> response;
  ByteString theData;

  @Before
  public void setUp() throws Exception {
    pathArgs = ImmutableMap.of("arg", "one", "and", "value2");

    request = mock(Request.class);
    //noinspection unchecked
    syncEndpointHandler = mock(SyncHandler.class);
    //noinspection unchecked
    asyncHandler = mock(AsyncHandler.class);

    Request request = Request.forUri("http://foo");
    requestContext = RequestContexts.create(request, mock(Client.class), pathArgs);

    theData = ByteString.encodeUtf8("theString");
    response = Response.forPayload(theData);
  }

  @Test
  public void shouldReturnResultOfSyncRoute() throws Exception {
    when(syncEndpointHandler.invoke(requestContext))
        .thenReturn(response);

    endpoint = new RouteEndpoint(Route.sync("GET", "http://foo", syncEndpointHandler));

    Response<?> actualResponse =
        endpoint.invoke(requestContext).toCompletableFuture().get();
    assertThat(actualResponse.payload(), equalTo(Optional.of(theData)));
  }

  @Test
  public void shouldReturnResultOfAsyncRoute() throws Exception {
    when(asyncHandler.invoke(requestContext))
        .thenReturn(completedFuture(response));

    endpoint = new RouteEndpoint(Route.create("GET", "http://foo", asyncHandler));

    Response<?> actualResponse = endpoint.invoke(requestContext).toCompletableFuture().get();
    assertThat(actualResponse.payload(), equalTo(Optional.of(theData)));
  }

  @Test
  public void shouldIncludeDocstringInEndpointInfo() throws Exception {
    endpoint = new RouteEndpoint(Route.sync("GET", "http://blah", ctx -> Response.<ByteString>ok())
                                     .withDocString("summarium", "this is a kewl description"));

    assertThat(endpoint.info().getDocString(),
               equalTo(Optional.of(DocString.doc("summarium", "this is a kewl description"))));
  }
}
