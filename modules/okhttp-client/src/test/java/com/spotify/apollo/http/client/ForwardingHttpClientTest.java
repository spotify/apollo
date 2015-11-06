package com.spotify.apollo.http.client;

import com.spotify.apollo.Request;
import com.spotify.apollo.Response;

import org.junit.Test;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import okio.ByteString;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class ForwardingHttpClientTest {

  @Test
  public void testSendHttp() throws Exception {
    final ForwardingHttpClient sut = ForwardingHttpClient.create(
        (request, incoming) -> {
          fail("Wrong client called");
          return null; // Unreachable
        },
        (request, incoming) -> CompletableFuture.completedFuture(
            Response.forPayload(ByteString.encodeUtf8("ok"))));

    final Response<ByteString> response =
        sut.send(Request.forUri("http://spoti.fi/bar"), Optional.empty()).toCompletableFuture()
            .get();

    assertThat(response.payload(), is(Optional.of(ByteString.encodeUtf8("ok"))));
  }

  @Test
  public void testSendOther() throws Exception {
    final ForwardingHttpClient sut = ForwardingHttpClient.create(
        (request, incoming) -> CompletableFuture.completedFuture(
            Response.forPayload(ByteString.encodeUtf8("ok"))),
        (request, incoming) -> {
          fail("Wrong client called");
          return null; // Unreachable
        });

    final Response<ByteString> response =
        sut.send(Request.forUri("teapot:stumpton231"), Optional.empty()).toCompletableFuture()
            .get();

    assertThat(response.payload(), is(Optional.of(ByteString.encodeUtf8("ok"))));
  }
}
