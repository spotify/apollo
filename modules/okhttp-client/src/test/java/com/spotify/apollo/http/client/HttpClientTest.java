/*
 * -\-\-
 * Spotify Apollo okhttp Client Module
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
package com.spotify.apollo.http.client;

import com.spotify.apollo.Request;
import com.spotify.apollo.Response;
import com.spotify.apollo.StatusType;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockserver.client.server.MockServerClient;
import org.mockserver.junit.MockServerRule;

import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.AbstractMap.SimpleEntry;
import java.util.Optional;

import okio.ByteString;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.mockserver.model.HttpCallback.callback;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class HttpClientTest {

  @Rule
  public final MockServerRule mockServerRule = new MockServerRule(this);

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  // this field gets set by the MockServerRule
  @SuppressWarnings("unused")
  private MockServerClient mockServerClient;

  @Test
  public void testSend() throws Exception {
    mockServerClient.when(
        request()
            .withMethod("GET")
            .withPath("/foo.php")
            .withQueryStringParameter("bar", "baz")
            .withQueryStringParameter("qur", "quz")
    ).respond(
        response()
            .withStatusCode(204)
    );

    String uri = format("http://localhost:%d/foo.php?bar=baz&qur=quz", mockServerRule.getHttpPort());
    Request request = Request.forUri(uri, "GET");
    Response<ByteString> response = HttpClient.createUnconfigured()
        .send(request, empty())
        .toCompletableFuture().get();

    assertThat(response.status(), withCode(204));
    assertThat(response.payload(), is(empty()));
  }

  @Test
  public void testSendWithCustomHeader() throws Exception {
    mockServerClient.when(
        request()
            .withMethod("GET")
            .withPath("/foo.php")
            .withHeader("x-my-special-header", "yes")
    ).respond(
        response()
            .withStatusCode(200)
            .withHeader("x-got-the-special-header", "yup")
    );

    String uri =  format("http://localhost:%d/foo.php", mockServerRule.getHttpPort());
    Response<ByteString> response = HttpClient.createUnconfigured()
        .send(Request.forUri(uri, "GET").withHeader("x-my-special-header", "yes"), empty())
        .toCompletableFuture().get();

    assertThat(response.status(), withCode(200));
    assertThat(response.header("x-got-the-special-header").get(), equalTo("yup"));
  }

  @Test
  public void testSendWithBody() throws Exception {
    mockServerClient.when(
        request()
            .withMethod("POST")
            .withPath("/foo.php")
            .withQueryStringParameter("bar", "baz")
            .withQueryStringParameter("qur", "quz")
            .withHeader("Content-Type", "application/x-spotify-greeting")
            .withBody("hello")
    ).respond(
        response()
            .withStatusCode(200)
            .withHeader("Content-Type", "application/x-spotify-location")
            .withHeader("Vary", "Content-Type")
            .withHeader("Vary", "Accept")
            .withBody("world")
    );

    String uri = format("http://localhost:%d/foo.php?bar=baz&qur=quz", mockServerRule.getHttpPort());
    Request request = Request.forUri(uri, "POST")
        .withHeader("Content-Type", "application/x-spotify-greeting")
        .withPayload(ByteString.encodeUtf8("hello"));

    Response<ByteString> response = HttpClient.createUnconfigured()
        .send(request, empty())
        .toCompletableFuture().get();

    assertThat(response.status(), withCode(200));
    assertThat(response.headerEntries(), allOf(
                   hasItem(new SimpleEntry<>("Content-Type", "application/x-spotify-location")),
                   hasItem(new SimpleEntry<>("Vary", "Content-Type, Accept"))
               ));
    assertThat(response.payload(), is(Optional.of(ByteString.encodeUtf8("world"))));
  }

  @Test
  public void testTimeout() throws Exception {
    mockServerClient.when(
        request()
        .withMethod("GET")
        .withPath("/foo.php")
    ).callback(
        callback()
        .withCallbackClass(SleepCallback.class.getCanonicalName())
    );

    String uri = format("http://localhost:%d/foo.php", mockServerRule.getHttpPort());
    Request request = Request.forUri(uri, "GET");

    Response<ByteString> response = HttpClient.createUnconfigured()
        .send(request, empty())
        .toCompletableFuture().get();

    assertThat(response.status(), withCode(200));
  }

  @Test
  public void testTimeoutFail() throws Exception {
    mockServerClient.when(
        request()
            .withMethod("GET")
            .withPath("/foo.php")
    ).callback(
        callback()
            .withCallbackClass(SleepCallback.class.getCanonicalName())
    );

    String uri = format("http://localhost:%d/foo.php", mockServerRule.getHttpPort());
    Request request = Request.forUri(uri, "GET").withTtl(Duration.ofMillis(200));

    thrown.expect(hasCause(instanceOf(SocketTimeoutException.class)));
      HttpClient.createUnconfigured()
          .send(request, empty())
          .toCompletableFuture().get();
  }

  @Test
  public void testAuthContextPropagation() throws Exception {
    mockServerClient.when(
        request().withHeader("Authorization", "Basic dXNlcjpwYXNz")
    ).respond(
        response().withHeader("x-auth-was-fine", "yes")
    );

    String uri = format("http://localhost:%d/foo.php", mockServerRule.getHttpPort());
    Request request = Request.forUri(uri, "GET");

    Request originalRequest = Request
        .forUri("http://original.uri/")
        .withHeader("Authorization", "Basic dXNlcjpwYXNz");

    final Response<ByteString> response = HttpClient.createUnconfigured()
        .send(request, Optional.of(originalRequest))
        .toCompletableFuture().get();

    assertThat(response.header("x-auth-was-fine").get(), equalTo("yes"));
  }

  @Test
  public void testSendWeirdStatus() throws Exception {
    mockServerClient.when(
        request()
            .withMethod("GET")
            .withPath("/foo.php")
    ).respond(
        response()
            .withStatusCode(299)
    );

    String uri = format("http://localhost:%d/foo.php", mockServerRule.getHttpPort());
    Request request = Request.forUri(uri, "GET");
    final Response<ByteString> response = HttpClient.createUnconfigured()
        .send(request, empty())
        .toCompletableFuture().get();

    assertThat(response.status(), withCode(299));
    assertThat(response.payload(), is(empty()));
  }

  private static Matcher<StatusType> withCode(int code) {
    return new TypeSafeMatcher<StatusType>() {
      @Override
      protected boolean matchesSafely(StatusType item) {
        return item.code() == code;
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("a status type with status code equals to ").appendValue(code);
      }

      @Override
      protected void describeMismatchSafely(StatusType item, Description mismatchDescription) {
        mismatchDescription.appendText("the status code was ").appendValue(item.code());
      }
    };
  }

  private static Matcher<Throwable> hasCause(Matcher<Throwable> expected) {
    return new TypeSafeMatcher<Throwable>() {
      @Override
      protected boolean matchesSafely(Throwable item) {
        for (Throwable cause = item ; cause != null ; cause = cause.getCause()) {
          if (expected.matches(cause)) {
            return true;
          }
        }

        return false;
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("with parent cause " + expected);
      }
    };
  }
}
