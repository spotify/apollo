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
import com.spotify.apollo.dispatch.Endpoint;
import com.spotify.apollo.request.RequestContexts;
import com.spotify.apollo.request.RequestMetadataImpl;

import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import okio.ByteString;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

@RunWith(Theories.class)
public class RouteRuleBuilderTest {

  private static final String BASE_URI = "http://service";

  private static final String HANDLER1_PATTERN = "%s: %s";

  /** mocks created in {@link #request(String, String)} */
  Client client;

  /** manually created spies in {@link #request(String, String)} */
  RequestContext requestContext;
  Request message;

  @DataPoints
  public static String[] methods() {
    return new String[] {"GET", "POST", "PUT", "DELETE"};
  }

  @DataPoint
  public static RouteProvider provider() {
    return () -> Stream.of(
        Route.sync("GET", "/test/<arg>", newHandler1("get-method")),
        Route.sync("POST", "/test/<arg>", newHandler1("post-method")),
        Route.sync("PUT", "/test/<arg>", newHandler1("put-method")),
        Route.sync("DELETE", "/test/<arg>", newHandler1("delete-method"))
    );
  }

  @Theory
  public void shouldCreateRule(String method) throws Exception {
    RouteProvider provider =  () -> Stream.of(
        Route.sync(method, "/test/<arg>", newHandler1("foo"))
    );

    Request request = request(method, "/test/" + "bar");
    ApplicationRouter<Endpoint> router = Routers.newRouterFromInspecting(provider);

    assertRequestResponse(router, "foo", "bar", request);
  }

  @Theory
  public void helperMethodsShouldProduceCorrectRoutes(String method, RouteProvider provider) throws Exception {
    ApplicationRouter<Endpoint> router = Routers.newRouterFromInspecting(provider);

    String lowerCaseMethod = method.toLowerCase();
    assertRequestResponse(router, lowerCaseMethod + "-method", "a-" + lowerCaseMethod,
                          request(method, "/test/" + "a-" + lowerCaseMethod));
  }

  private void assertRequestResponse(ApplicationRouter<Endpoint> router,
                                     String prefix, String arg, Request message) throws Exception {

    Optional<RuleMatch<Endpoint>> match = router.match(message);
    assertTrue(match.isPresent());

    String response = string(handle(match.get()));
    assertThat(response, is(prefix + ": " + arg));
  }

  private Response<ByteString> handle(RuleMatch<Endpoint> match)
      throws ExecutionException, InterruptedException {
    final Endpoint endpoint = match.getRule().getTarget();
    final Map<String, String> parsedPathArguments = match.parsedPathArguments();

    return endpoint.invoke(
        RequestContexts.create(
            requestContext.request(), requestContext.requestScopedClient(), parsedPathArguments,
            RequestMetadataImpl.create(getClass(), Instant.EPOCH, "non", Optional.empty(), Optional.empty()))
    ).toCompletableFuture().get();
  }

  private Request request(String method, String uri) {
    client = mock(Client.class);
    message = Request.forUri(BASE_URI + uri, method);
    requestContext = RequestContexts.create(message, client, Collections.emptyMap(),
                                            RequestMetadataImpl.create(getClass(), Instant.EPOCH, "nonee", Optional.empty(), Optional.empty()));

    return message;
  }

  private static String string(Response<ByteString> message) {
    return message.payload().get().utf8();
  }

  private static SyncHandler<Response<String>> newHandler1(String enclose) {
    return requestContext -> Response.forPayload(
        format(HANDLER1_PATTERN, enclose, requestContext.pathArgs().get("arg")));
  }
}
