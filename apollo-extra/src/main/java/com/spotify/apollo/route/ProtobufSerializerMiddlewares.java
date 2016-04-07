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

import com.google.protobuf.MessageLite;
import com.spotify.apollo.Response;
import okio.ByteString;

import java.util.Objects;

public class ProtobufSerializerMiddlewares {
  private ProtobufSerializerMiddlewares() {
    // prevent instantiation
  }
  private static final String CONTENT_TYPE = "Content-Type";
  private static final String CONTENT_TYPE_PROTOBUF = "application/octet-stream";

  private static <T extends MessageLite> ByteString serialize(T object) {
    Objects.requireNonNull(object, "protoMessage");
    return ByteString.of(object.toByteArray());
  }

  /**
   * Middleware that serializes the result of the inner protobuf object handler and sets the
   * Content-Type header to application/octet-stream.
   */
  public static <T extends MessageLite> Middleware<AsyncHandler<T>, AsyncHandler<Response<ByteString>>>
  protobufSerialize() {
    return handler ->
        requestContext -> handler.invoke(requestContext)
            .thenApply(result -> Response
                .forPayload(serialize(result))
                .withHeader(CONTENT_TYPE, CONTENT_TYPE_PROTOBUF));
  }

  /**
   * Middleware that serializes the payload of the protobuf result response of the inner handler
   * and sets the Content-Type header to application/octet-stream.
   */
  public static <T extends MessageLite> Middleware<AsyncHandler<Response<T>>, AsyncHandler<Response<ByteString>>>
  protobufSerializeResponse() {
    return handler ->
        requestContext -> handler.invoke(requestContext)
          .thenApply(response -> response
            .withPayload(response.payload().map(ProtobufSerializerMiddlewares::serialize).orElse(null))
            .withHeader(CONTENT_TYPE, CONTENT_TYPE_PROTOBUF));
  }

  public static <T extends MessageLite> Middleware<SyncHandler<T>, AsyncHandler<Response<ByteString>>>
  protobufSerializeSync() {
    Middleware<SyncHandler<T>, AsyncHandler<T>> syncToAsync = Middleware::syncToAsync;
    return syncToAsync.and(protobufSerialize());
  }

  public static <T extends MessageLite> Middleware<SyncHandler<Response<T>>, AsyncHandler<Response<ByteString>>>
  protobufSerializeResponseSync() {
    Middleware<SyncHandler<Response<T>>, AsyncHandler<Response<T>>> syncToAsync = Middleware::syncToAsync;
    return syncToAsync.and(protobufSerializeResponse());
  }
}

