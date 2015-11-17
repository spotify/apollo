/*
 * -\-\-
 * Spotify Apollo API Implementations
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

import com.spotify.apollo.Request;
import com.spotify.apollo.Response;
import com.spotify.apollo.Serializer;
import com.spotify.apollo.Status;
import com.spotify.apollo.StatusType;
import com.spotify.apollo.serialization.AutoSerializer;

import java.util.Optional;
import java.util.function.Function;

import okio.ByteString;

/**
 * Contains utility {@link Middleware} implementations.
 */
public final class Middlewares {

  private static final String CONTENT_TYPE = "Content-Type";

  private Middlewares() {
    // prevent instantiation
  }

  /**
   * Converts an AsyncHandler with unspecified return type to one that returns {@code
   * Response<ByteString>}. This is done through a best-effort mechanism.
   *
   * Using this middleware has the effect that your code is no longer typesafe, so it may be
   * preferable to write your own middleware that converts the domain object your handler returns
   * into a {@code Response<ByteString>}.
   */
  public static <T> AsyncHandler<Response<ByteString>> autoSerialize(AsyncHandler<T> inner) {
    return serialize(new AutoSerializer()).apply(inner);
  }

  /**
   * Applies logic to the inner {@link AsyncHandler} that makes it conform to the semantics
   * specified in HTTP regarding when to return response bodies, Content-Length headers, etc.
   */
  public static AsyncHandler<Response<ByteString>> httpPayloadSemantics(
      AsyncHandler<Response<ByteString>> inner) {

    return requestContext ->
        inner.invoke(requestContext)
            .thenApply(applyHttpPayloadSemantics(requestContext.request()));
  }

  /**
   * Middleware that adds the ability to set the response's Content-Type header to a defined
   * value.
   *
   * This middleware is type-unsafe, and it might be a better idea to set the content type directly
   * in your own middleware that does response serialization.
   */
  public static Middleware<AsyncHandler<?>, AsyncHandler<Response<?>>> replyContentType(
      String contentType) {

    return inner -> requestContext ->
        ensureResponse(inner)
            .invoke(requestContext)
            .thenApply(response -> response.withHeader(CONTENT_TYPE, contentType));
  }

  /**
   * Middleware that applies the supplied serializer to the result of the inner handler,
   * changing the payload and optionally the Content-Type header.
   *
   * This middleware is type-unsafe, and it might be better to write your own middleware that does
   * serialization.
   */
  public static Middleware<AsyncHandler<?>, AsyncHandler<Response<ByteString>>> serialize(
      Serializer serializer) {

    return inner -> requestContext ->
        ensureResponse(inner)
            .invoke(requestContext)
            .thenApply(serializePayload(serializer, requestContext.request()));
  }

  /**
   * Returns the default middlewares applied by Apollo to routes supplied by a {@link RouteProvider}.
   */
  public static Middleware<AsyncHandler<?>, AsyncHandler<Response<ByteString>>> apolloDefaults() {
    return ((Middleware<AsyncHandler<?>, AsyncHandler<Response<ByteString>>>) Middlewares::autoSerialize)
        .and(Middlewares::httpPayloadSemantics);
  }

  private static <T> AsyncHandler<Response<T>> ensureResponse(AsyncHandler<T> inner) {
    return requestContext ->
        inner.invoke(requestContext)
            .thenApply(Middlewares::ensureResponse);
  }

  private static Function<Response<ByteString>, Response<ByteString>> applyHttpPayloadSemantics(
      Request request) {

    return response -> {
      Response<ByteString> result = response;
      Optional<ByteString> payload = response.payload();
      if (setContentLengthForStatus(response.status())) {
        int payloadSize = payload.isPresent() ? payload.get().size() : 0;
        result = result.withHeader("Content-Length", String.valueOf(payloadSize));
      }

      if (!setPayloadForMethod(request.method()) ||
          !setPayloadForStatus(response.status())) {
        result = result.withPayload(null);
      }

      return result;
    };
  }

  // see http://www.w3.org/Protocols/rfc2616/rfc2616-sec4.html#sec4.3
  private static boolean setPayloadForStatus(StatusType statusType) {
    return statusType.code() != Status.NOT_MODIFIED.code() &&
           statusType.code() != Status.NO_CONTENT.code() &&
           statusType.family() != StatusType.Family.INFORMATIONAL;
  }

  private static boolean setPayloadForMethod(String method) {
    return !"HEAD".equalsIgnoreCase(method);
  }

  // see http://www.w3.org/Protocols/rfc2616/rfc2616-sec4.html#sec4.3
  private static boolean setContentLengthForStatus(StatusType statusType) {
    return setPayloadForStatus(statusType);
  }

  private static <T> Function<Response<T>, Response<ByteString>> serializePayload(
      Serializer serializer, Request request) {

    return response -> {
      if (!response.payload().isPresent()) {
        // no payload, so this cast is safe to do
        //noinspection unchecked
        return (Response<ByteString>) response;
      }

      final T payloadObject = response.payload().get();
      final Serializer.Payload payload =
          serializer.serialize(request, payloadObject);

      if (payload.contentType().isPresent()) {
        response = response.withHeader(CONTENT_TYPE, payload.contentType().get());
      }

      return response.withPayload(payload.byteString());
    };
  }

  private static <T> Response<T> ensureResponse(T t) {
    if (t instanceof Response) {
      //noinspection unchecked
      return (Response<T>) t;
    } else {
      return Response.forPayload(t);
    }
  }
}
