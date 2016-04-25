/*
 * -\-\-
 * Spotify Apollo Testing Helpers
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
package com.spotify.apollo.test;

import com.google.common.collect.ImmutableSet;

import com.spotify.apollo.Request;
import com.spotify.apollo.Response;
import com.spotify.apollo.Status;
import com.spotify.apollo.StatusType;
import com.spotify.apollo.test.response.ResponseSource;
import com.spotify.apollo.test.response.ResponseWithDelay;
import com.spotify.apollo.test.response.Responses;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

import okio.ByteString;

import static com.spotify.apollo.test.unit.StatusTypeMatchers.withCode;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.toSet;
import static okio.ByteString.encodeUtf8;
import static org.hamcrest.CoreMatchers.any;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class StubClientTest {

  StubClient stubClient;

  @Rule
  public ExpectedException exception = ExpectedException.none();
  private static final ResponseWithDelay HELLO_WORLD =
      ResponseWithDelay.forResponse(Response.forPayload(encodeUtf8("Hello World")));

  @Before
  public void setUp() throws Exception {
    stubClient = new StubClient();
  }

  private CompletionStage<String> getStringFromPing() {
    return getString("http://ping");
  }

  private CompletionStage<String> getString(String uri) {
    return getResponse(uri).thenApply(r -> r.payload().get().utf8());
  }

  private CompletionStage<Response<ByteString>> getResponseFromPing() {
    return getResponse("http://ping");
  }

  private CompletionStage<Response<ByteString>> getResponse(String uri) {
    return stubClient.send(Request.forUri(uri));
  }

  @Test
  public void shouldSimulateDelay() throws Exception {
    stubClient
        .respond(Response.forPayload(encodeUtf8("Hello World")))
        .in(400, MILLISECONDS).to("http://ping");

    final long t0 = System.currentTimeMillis();
    final CompletionStage<String> call = getStringFromPing();
    final String reply = call.toCompletableFuture().get();
    final long elapsed = System.currentTimeMillis() - t0;

    assertThat(reply, is("Hello World"));
    // this can be really slow on the build server when it's under heavy load.
    assertThat(elapsed, is(both(lessThan(2000L)).and(greaterThan(350L))));
  }

  @Test
  public void shouldReturnConfiguredStatusCode() throws Exception {
    stubClient.respond(Response.of(Status.IM_A_TEAPOT, encodeUtf8("Hello World"))).to("http://ping");

    Response<ByteString> response = getResponseFromPing().toCompletableFuture().get();
    assertThat(response.status(), is(Status.IM_A_TEAPOT));
  }

  @Test
  public void shouldReplyWithStatusOnly() throws Exception {
    stubClient.respond(Response.forStatus(Status.IM_A_TEAPOT)).to("http://ping");

    Response<ByteString> response = getResponseFromPing().toCompletableFuture().get();
    assertThat(response.status(), is(Status.IM_A_TEAPOT));
  }

  @Test
  public void shouldSupportNoPayloads() throws Exception {
    stubClient.respond(Response.forStatus(Status.IM_A_TEAPOT)).to("http://ping");

    Response<ByteString> response = getResponseFromPing().toCompletableFuture().get();
    assertThat(response.payload().isPresent(), is(false));
  }

  @Test
  public void shouldReturnStatusCodeIntegers() throws Exception {
    ResponseWithDelay response = ResponseWithDelay.forResponse(
        Response.of(Status.createForCode(666), encodeUtf8("constant response")));

    ResponseSource responses = Responses.constant(response);
    stubClient.respond(responses).to("http://ping");

    Response<ByteString> reply = getResponseFromPing().toCompletableFuture().get();
    assertThat(reply.status(), withCode(666));
  }

  @Test
  public void shouldReturnConstantReplies() throws Exception {
    ResponseWithDelay response = ResponseWithDelay.forResponse(
        Response.forPayload(encodeUtf8("constant response")));
    ResponseSource responses = Responses.constant(response);
    stubClient.respond(responses).to("http://ping");

    assertThat(getStringFromPing().toCompletableFuture().get(), is("constant response"));
    assertThat(getStringFromPing().toCompletableFuture().get(), is("constant response"));
  }

  @Test
  public void shouldReturnIterativeReplies() throws Exception {
    ResponseWithDelay response1 = ResponseWithDelay.forResponse(
        Response.forPayload(encodeUtf8("first response")));
    ResponseWithDelay response2 = ResponseWithDelay.forResponse(
        Response.forPayload(encodeUtf8("second response")));
    List<ResponseWithDelay> responses = Arrays.asList(response1, response2);
    ResponseSource responseSequence = Responses.sequence(responses);
    stubClient.respond(responseSequence).to("http://ping");

    assertThat(getStringFromPing().toCompletableFuture().get(), is("first response"));
    assertThat(getStringFromPing().toCompletableFuture().get(), is("second response"));
  }

  @Test
  public void shouldReturnIterativeRepliesWithVaryingStatusCodes() throws Exception {
    ResponseWithDelay response1 = ResponseWithDelay.forResponse(
        Response.of(Status.ACCEPTED, encodeUtf8("first response")));
    ResponseWithDelay response2 = ResponseWithDelay.forResponse(
        Response.of(Status.FORBIDDEN, encodeUtf8("second response")));
    List<ResponseWithDelay> responses = Arrays.asList(response1, response2);
    ResponseSource responseSequence = Responses.sequence(responses);
    stubClient.respond(responseSequence).to("http://ping");

    CompletionStage<Response<ByteString>> reply1 = getResponseFromPing();
    CompletionStage<Response<ByteString>> reply2 = getResponseFromPing();

    Response<ByteString> message1 = reply1.toCompletableFuture().get();
    Response<ByteString> message2 = reply2.toCompletableFuture().get();

    assertThat(message1.status(), is(Status.ACCEPTED));
    assertThat(message2.status(), is(Status.FORBIDDEN));

    assertThat(message1.payload().get().utf8(), is("first response"));
    assertThat(message2.payload().get().utf8(), is("second response"));
  }

  @Test
  public void shouldDisallowInfiniteIterativeReplies() throws Exception {
    ResponseWithDelay response1 = ResponseWithDelay.forResponse(
        Response.forPayload(encodeUtf8("first response")));
    ResponseWithDelay response2 = ResponseWithDelay.forResponse(
        Response.forPayload(encodeUtf8("second response")));
    List<ResponseWithDelay> responses = Arrays.asList(response1, response2);
    ResponseSource constantResponses = Responses.sequence(responses);
    stubClient.respond(constantResponses).to("http://ping");

    assertThat(getStringFromPing().toCompletableFuture().get(), is("first response"));
    assertThat(getStringFromPing().toCompletableFuture().get(), is("second response"));

    try {
      getStringFromPing().toCompletableFuture().get();
      fail();
    } catch (IllegalStateException e) {
      assertThat(e.getMessage(), is("No more responses specified!"));
    }
  }

  @Test
  public void shouldRespectCustomRequestMatcher() throws Exception {
    final String mockedUri = "http://ping";
    final String effectiveUri = "http://ping?key=value";

    Matcher<Request> requestMatcher = uriStartsWith(mockedUri);
    stubClient.respond(Responses.constant(HELLO_WORLD)).to(requestMatcher);

    final String reply = getString(effectiveUri).toCompletableFuture().get();
    assertThat(reply, is("Hello World"));
  }

  @Test
  public void shouldClearSetupRequestsOnClear() throws Throwable {
    stubClient.respond(Response.forPayload(encodeUtf8("Hello World"))).to("http://ping");

    stubClient.clear();

    CompletionStage<String> future = getString("http://ping");

    exception.expect(isA(StubClient.NoMatchingResponseFoundException.class));
    try {
      future.toCompletableFuture().get();
    } catch (ExecutionException ee) {
      throw ee.getCause();
    }
    fail("should throw");
  }

  @Test
  public void shouldDisallowChangingDelayForResponseSource() throws Exception {
    StubClient.StubbedResponseBuilder builder = stubClient.respond(Responses.constant(HELLO_WORLD));

    exception.expect(IllegalStateException.class);

    builder.in(13, MILLISECONDS);
  }


  @Test
  public void shouldSupportHeadersForResponses() throws Exception {
    stubClient.respond(Response.<ByteString>ok()
                           .withHeader("foo", "bar"))
        .to("http://ping");

    Response reply = getResponse("http://ping").toCompletableFuture().get();
    assertThat(reply.headers().get("foo"), is(Optional.of("bar")));
  }

  @Test
  public void shouldDisallowAddingDelayForResponseSource() throws Exception {
    StubClient.StubbedResponseBuilder builder = stubClient.respond(Responses.constant(HELLO_WORLD));

    exception.expect(IllegalStateException.class);

    builder.in(14, MILLISECONDS);
  }

  @Test
  public void shouldSupportTrackingRequests() throws Exception {
    stubClient.respond(Response.ok()).to(any(Request.class));

    getResponse("http://ping").toCompletableFuture().get();
    getResponse("http://pong").toCompletableFuture().get();

    Set<String> uris = stubClient.sentRequests().stream()
        .map(Request::uri)
        .collect(toSet());

    assertThat(uris, equalTo(ImmutableSet.of("http://ping", "http://pong")));
  }

  @Test
  public void shouldSupportClearingRequests() throws Exception {
    stubClient.respond(Response.ok()).to(any(Request.class));

    getResponse("http://ping").toCompletableFuture().get();

    stubClient.clearRequests();

    getResponse("http://pong").toCompletableFuture().get();

    Set<String> uris = stubClient.sentRequests().stream()
        .map(Request::uri)
        .collect(toSet());

    assertThat(uris, equalTo(ImmutableSet.of("http://pong")));
  }

  @Test
  public void shouldSupportTrackingRequestsAndResponses() throws Exception {
    stubClient.respond(Response.ok()).to("http://ping");
    stubClient.respond(Response.forStatus(Status.BAD_GATEWAY)).to("http://pong");

    getResponse("http://ping").toCompletableFuture().get();
    getResponse("http://pong").toCompletableFuture().get();

    Set<StatusType> statii = stubClient.requestsAndResponses().stream()
        .map(requestAndResponse -> requestAndResponse.response().status())
        .collect(toSet());

    assertThat(statii, equalTo(ImmutableSet.of(Status.OK, Status.BAD_GATEWAY)));
  }

  private static Matcher<Request> uriStartsWith(String uriPrefix) {
    return new TypeSafeMatcher<Request>() {
      @Override
      protected boolean matchesSafely(Request request) {
        return startsWith(uriPrefix).matches(request.uri());
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("uri starts with");
        description.appendValue(uriPrefix);
      }
    };
  }


}
