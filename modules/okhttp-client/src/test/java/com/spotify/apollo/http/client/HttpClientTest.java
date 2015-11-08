package com.spotify.apollo.http.client;

import com.spotify.apollo.Request;
import com.spotify.apollo.Response;
import com.spotify.apollo.StatusType;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.mockserver.client.server.MockServerClient;
import org.mockserver.junit.MockServerRule;

import java.util.Optional;

import okio.ByteString;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
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

    assertThat(response.statusCode(), withCode(204));
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

    assertThat(response.statusCode(), withCode(200));
    assertThat(response.headers(), allOf(
                   hasEntry("Content-Type", "application/x-spotify-location"),
                   hasEntry("Vary", "Content-Type, Accept")
               ));
    assertThat(response.payload(), is(Optional.of(ByteString.encodeUtf8("world"))));
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

    assertThat(response.statusCode(), withCode(299));
    assertThat(response.payload(), is(empty()));
  }

  private static Matcher<StatusType> withCode(int code) {
    return new TypeSafeMatcher<StatusType>() {
      @Override
      protected boolean matchesSafely(StatusType item) {
        return item.statusCode() == code;
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("a status type with status code equals to ").appendValue(code);
      }

      @Override
      protected void describeMismatchSafely(StatusType item, Description mismatchDescription) {
        mismatchDescription.appendText("the status code was ").appendValue(item.statusCode());
      }
    };
  }
}
