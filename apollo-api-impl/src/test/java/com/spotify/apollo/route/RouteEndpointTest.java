/*
 * -\-\-
 * Spotify Apollo API Implementations
 * --
 * Copyright (C) 2013 - 2015 Spotify AB
 * --
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -/-/-
 */
package com.spotify.apollo.route;

import com.spotify.apollo.Client;
import com.spotify.apollo.Request;
import com.spotify.apollo.RequestContext;
import com.spotify.apollo.Response;
import com.spotify.apollo.request.RequestContexts;
import com.spotify.apollo.request.RequestMetadataImpl;
import com.spotify.apollo.route.Route.DocString;

import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ImmutableMap;
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
    requestContext = RequestContexts.create(request, mock(Client.class), pathArgs,
                                            0L,
                                            RequestMetadataImpl.create(Instant.EPOCH, Optional.empty(), Optional.empty()));

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
