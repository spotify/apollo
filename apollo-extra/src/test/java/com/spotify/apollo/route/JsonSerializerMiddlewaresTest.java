/*
 * Copyright © 2015 Spotify AB
 */
package com.spotify.apollo.route;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.spotify.apollo.Response;
import com.spotify.apollo.Status;

import org.junit.Test;

import okio.ByteString;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class JsonSerializerMiddlewaresTest {
  private static final ObjectWriter WRITER = new ObjectMapper().writer();

  private static class TestBean {
    public int getX() { return 3; }
  }

  private static void checkPayloadAndContentType(Response<ByteString> response) {
    assertThat(response.payload().get().utf8(), equalTo("{\"x\":3}"));
    assertThat(response.headers().get("Content-Type"), equalTo("application/json; charset=UTF8"));
  }

  @Test
  public void shouldJsonSerialize() throws Exception {
    Response<ByteString> response =
        Route.sync("GET", "/foo", rq -> new TestBean())
        .withMiddleware(JsonSerializerMiddlewares.jsonSerialize(WRITER))
        .handler().invoke(null).toCompletableFuture().get();

    checkPayloadAndContentType(response);
  }

  @Test
  public void shouldJsonSerializeResponse() throws Exception {
    Response<ByteString> response =
        Route.sync("GET", "/foo",
                   rq -> Response.forStatus(Status.CONFLICT).withPayload(new TestBean()))
        .withMiddleware(JsonSerializerMiddlewares.jsonSerializeResponse(WRITER))
        .handler().invoke(null).toCompletableFuture().get();

    checkPayloadAndContentType(response);
    assertThat(response.statusCode(), equalTo(Status.CONFLICT));
  }

  @Test
  public void shouldJsonSerializeSync() throws Exception {
    Middleware<SyncHandler<TestBean>, AsyncHandler<Response<ByteString>>> sync =
        JsonSerializerMiddlewares.jsonSerializeSync(WRITER);
    Response<ByteString> response = sync.apply(rq -> new TestBean())
        .invoke(null).toCompletableFuture().get();

    checkPayloadAndContentType(response);
  }

  @Test
  public void shouldJsonSerializeResponseSync() throws Exception {
    Middleware<SyncHandler<Response<TestBean>>, AsyncHandler<Response<ByteString>>> sync =
        JsonSerializerMiddlewares.jsonSerializeResponseSync(WRITER);
    Response<ByteString> response = sync.apply(
        rq -> Response.forStatus(Status.CONFLICT).withPayload(new TestBean()))
        .invoke(null).toCompletableFuture().get();

    checkPayloadAndContentType(response);
    assertThat(response.statusCode(), equalTo(Status.CONFLICT));
  }
}
