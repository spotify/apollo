/*
 * -\-\-
 * Spotify Apollo Extra
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.spotify.apollo.Response;
import com.spotify.apollo.Status;

import org.junit.Test;

import java.util.Optional;

import okio.ByteString;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class JsonSerializerMiddlewaresTest {
  private static final ObjectWriter WRITER = new ObjectMapper().writer();

  private static class TestBean {
    public int getX() { return 3; }
  }

  private static void checkPayloadAndContentType(Response<ByteString> response) {
    assertThat(response.payload().get().utf8(), equalTo("{\"x\":3}"));
    checkContentType(response);
  }

  private static void checkContentType(Response<ByteString> response) {
    assertThat(response.headers().get("Content-Type"),
               is(Optional.of("application/json; charset=UTF8")));
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
    assertThat(response.status(), equalTo(Status.CONFLICT));
  }

  @Test
  public void shouldJsonSerializeEmptyResponse() throws Exception {
    Response<ByteString> response =
        Route.sync("GET", "/foo",
                   rq -> Response.forStatus(Status.CONFLICT))
        .withMiddleware(JsonSerializerMiddlewares.jsonSerializeResponse(WRITER))
        .handler().invoke(null).toCompletableFuture().get();

    checkContentType(response);
    assertThat(response.payload(), equalTo(Optional.empty()));
    assertThat(response.status(), equalTo(Status.CONFLICT));
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
    assertThat(response.status(), equalTo(Status.CONFLICT));
  }
}
