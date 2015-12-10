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
import org.hamcrest.core.IsInstanceOf;
import org.junit.Rule;
import org.junit.Test;
import org.mockserver.client.server.MockServerClient;
import org.mockserver.junit.MockServerRule;

import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import okio.ByteString;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;
import static org.mockserver.model.HttpCallback.callback;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class HttpClientTest {

  @Rule
  public final MockServerRule mockServerRule = new MockServerRule(this);

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
    Response<ByteString> response = HttpClient.create()
        .send(request, empty())
        .toCompletableFuture().get();

    assertThat(response.status(), withCode(204));
    assertThat(response.payload(), is(empty()));
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

    Response<ByteString> response = HttpClient.create()
        .send(request, empty())
        .toCompletableFuture().get();

    assertThat(response.status(), withCode(200));
    assertThat(response.headers(), allOf(
                   hasEntry("Content-Type", "application/x-spotify-location"),
                   hasEntry("Vary", "Content-Type, Accept")
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

    Response<ByteString> response = HttpClient.create()
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
    Request request = Request.forUri(uri, "GET").withTtl(Duration.ofSeconds(1));

    try {
      HttpClient.create()
          .send(request, empty())
          .toCompletableFuture().get();
    } catch (InterruptedException e) {
      fail();
    } catch (ExecutionException e) {
      assertThat(e.getCause().getCause(), IsInstanceOf.instanceOf(SocketTimeoutException.class));
    }
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
    final Response<ByteString> response = HttpClient.create()
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
}
