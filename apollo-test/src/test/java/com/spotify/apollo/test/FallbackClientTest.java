package com.spotify.apollo.test;

import com.spotify.apollo.Request;
import com.spotify.apollo.Response;
import com.spotify.apollo.Status;
import com.spotify.apollo.environment.IncomingRequestAwareClient;

import org.junit.Before;
import org.junit.Test;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import okio.ByteString;

import static java.util.Optional.empty;
import static okio.ByteString.encodeUtf8;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class FallbackClientTest {

  FallbackClient fallbackClient;

  StubClient stubClient;

  @Before
  public void setUp() throws Exception {
    final IncomingRequestAwareClient client = new FixedReplyClient("I am pretty rigid");

    stubClient = new StubClient();
    fallbackClient = new FallbackClient(stubClient.asRequestAwareClient(), client);
  }

  private String call(String uri) throws Exception {
    return fallbackClient.send(Request.forUri(uri), empty()).thenApply(
        response -> {
          if (response.status() != Status.OK) {
            throw new IllegalStateException();
          }

          return response.payload().get().utf8();
        })
        .toCompletableFuture().get();
  }

  @Test
  public void shouldFallBackIfNoResponseFound() throws Exception {
    stubClient.respond(Response.forPayload(encodeUtf8("Hello World"))).to("http://ping");

    final String mockReply = call("http://ping");
    final String fixedReply1 = call("http://pong");
    final String fixedReply2 = call("http://bang");

    assertThat(mockReply, is("Hello World"));
    assertThat(fixedReply1, is("I am pretty rigid"));
    assertThat(fixedReply2, is("I am pretty rigid"));
  }

  private static final class FixedReplyClient implements IncomingRequestAwareClient {

    private final String reply;

    public FixedReplyClient(String reply) {
      this.reply = reply;
    }

    private Response<ByteString> reply() {
      return Response.forPayload(ByteString.encodeUtf8(reply));
    }

    @Override
    public CompletionStage<Response<ByteString>> send(Request request, Optional<Request> incoming) {
      return CompletableFuture.completedFuture(reply());
    }
  }
}
