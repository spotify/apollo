/*
 * Copyright © 2015 Spotify AB
 */
package com.spotify.apollo.route;

import com.google.common.base.Throwables;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.spotify.apollo.Response;

import okio.ByteString;

public class JsonSerializerMiddlewares {
  private JsonSerializerMiddlewares() {
    // prevent instantiation
  }

  private static final String CONTENT_TYPE = "Content-Type";
  private static final String JSON = "application/json; charset=UTF8";

  private static <T> ByteString serialize(ObjectWriter objectWriter, T object) {
    try {
      return ByteString.of(objectWriter.writeValueAsBytes(object));
    } catch (JsonProcessingException e) {
      throw Throwables.propagate(e);
    }
  }

  /**
   * Middleware that serializes the result of the inner handler using the supplied
   * {@link ObjectWriter}, and sets the Content-Type header to application/json.
   */
  public static <T> Middleware<AsyncHandler<T>, AsyncHandler<Response<ByteString>>>
  jsonSerialize(ObjectWriter objectWriter) {
   return handler ->
       requestContext -> handler.invoke(requestContext)
           .thenApply(result -> Response
               .forPayload(serialize(objectWriter, result))
               .withHeader(CONTENT_TYPE, JSON));
  }

  /**
   * Middleware that serializes the payload of the result response of the inner handler using
   * the supplied {@link ObjectWriter}, and sets the Content-Type header to application/json.
   */
  public static <T> Middleware<AsyncHandler<Response<T>>, AsyncHandler<Response<ByteString>>>
  jsonSerializeResponse(ObjectWriter objectWriter) {
   return handler ->
       requestContext -> handler.invoke(requestContext)
           .thenApply(response -> response
               .withPayload(serialize(objectWriter, response.payload().orElse(null)))
               .withHeader(CONTENT_TYPE, JSON));
  }

  public static <T> Middleware<SyncHandler<T>, AsyncHandler<Response<ByteString>>>
  jsonSerializeSync(ObjectWriter objectWriter) {
    Middleware<SyncHandler<T>, AsyncHandler<T>> syncToAsync = Middleware::syncToAsync;
    return syncToAsync.and(jsonSerialize(objectWriter));
  }

  public static <T> Middleware<SyncHandler<Response<T>>, AsyncHandler<Response<ByteString>>>
  jsonSerializeResponseSync(ObjectWriter objectWriter) {
    Middleware<SyncHandler<Response<T>>, AsyncHandler<Response<T>>> syncToAsync = Middleware::syncToAsync;
    return syncToAsync.and(jsonSerializeResponse(objectWriter));
  }
}
