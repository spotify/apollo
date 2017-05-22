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

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.Message;
import com.google.protobuf.UnknownFieldSet;
import com.spotify.apollo.Response;
import com.spotify.apollo.Status;
import okio.ByteString;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class ProtobufSerializerMiddlewaresTest {

  private static Message getMessage() {
    // from https://gist.github.com/nandub/950285
    // from http://stackoverflow.com/q/28161527
    DescriptorProtos.DescriptorProto des = DescriptorProtos.DescriptorProto.newBuilder().build();

    DynamicMessage.Builder builder = DynamicMessage.newBuilder(des);
    builder.setUnknownFields(UnknownFieldSet.newBuilder().mergeVarintField(1, 7).build());

    return builder.build();
  }

  private static void checkPayloadAndContentType(Response<ByteString> response) {
    assertThat(response.payload().get().toByteArray(), equalTo(getMessage().toByteArray()));
    assertThat(response.headers().get("Content-Type"), equalTo("application/octet-stream"));
  }

  @Test
  public void shouldProtobufSerialize() throws Exception {
    Response<ByteString> response =
        Route.sync("GET", "/foo", rq -> getMessage())
        .withMiddleware(ProtobufSerializerMiddlewares.protobufSerialize())
        .handler().invoke(null).toCompletableFuture().get();

    checkPayloadAndContentType(response);
  }

  @Test
  public void shouldProtobufSerializeResponse() throws Exception {
    Response<ByteString> response =
        Route.sync("GET", "/foo",
                   rq -> Response.forStatus(Status.CONFLICT).withPayload(getMessage()))
        .withMiddleware(ProtobufSerializerMiddlewares.protobufSerializeResponse())
        .handler().invoke(null).toCompletableFuture().get();

    checkPayloadAndContentType(response);
    assertThat(response.status(), equalTo(Status.CONFLICT));
  }

  @Test
  public void shouldProtobufSerializeSync() throws Exception {
    Middleware<SyncHandler<Message>, AsyncHandler<Response<ByteString>>> sync =
        ProtobufSerializerMiddlewares.protobufSerializeSync();
    Response<ByteString> response = sync.apply(rq -> getMessage())
        .invoke(null).toCompletableFuture().get();

    checkPayloadAndContentType(response);
  }

  @Test
  public void shouldProtobufSerializeResponseSync() throws Exception {
    Middleware<SyncHandler<Response<Message>>, AsyncHandler<Response<ByteString>>> sync =
        ProtobufSerializerMiddlewares.protobufSerializeResponseSync();
    Response<ByteString> response = sync.apply(
        rq -> Response.forStatus(Status.CONFLICT).withPayload(getMessage()))
        .invoke(null).toCompletableFuture().get();

    checkPayloadAndContentType(response);
    assertThat(response.status(), equalTo(Status.CONFLICT));
  }
}
