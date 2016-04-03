/*
 * -\-\-
 * Spotify Apollo Entity Middleware
 * --
 * Copyright (C) 2013 - 2016 Spotify AB
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
package com.spotify.apollo.entity;

import com.spotify.apollo.Response;
import com.spotify.apollo.Status;
import com.spotify.apollo.route.AsyncHandler;
import com.spotify.apollo.route.Middleware;
import com.spotify.apollo.route.SyncHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import okio.ByteString;

import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * A factory for creating middlewares that can be used to create routes that deal directly
 * with an api entity.
 *
 * A {@link EntityCodec} is used to define how to go between a {@link ByteString} and the
 * entity type used in your route handlers.
 */
class CodecEntityMiddleware implements EntityMiddleware {

  private static final Logger LOG = LoggerFactory.getLogger(CodecEntityMiddleware.class);

  private static final String CONTENT_TYPE = "Content-Type";

  private final EntityCodec codec;
  private final String contentType;

  CodecEntityMiddleware(EntityCodec codec) {
    this(codec, codec.defaultContentType());
  }

  CodecEntityMiddleware(EntityCodec codec, String contentType) {
    this.codec = Objects.requireNonNull(codec);
    this.contentType = Objects.requireNonNull(contentType);
  }

  @Override
  public <R> Middleware<SyncHandler<R>, SyncHandler<Response<ByteString>>>
  serializerDirect(Class<? extends R> entityClass) {
    return inner -> inner
        .map(this::ensureResponse)
        .map(serialize(entityClass));
  }

  @Override
  public <R> Middleware<SyncHandler<Response<R>>, SyncHandler<Response<ByteString>>>
  serializerResponse(Class<? extends R> entityClass) {
    return inner -> inner.map(serialize(entityClass));
  }

  @Override
  public <R> Middleware<AsyncHandler<R>, AsyncHandler<Response<ByteString>>>
  asyncSerializerDirect(Class<? extends R> entityClass) {
    return inner -> inner
        .map(this::ensureResponse)
        .map(serialize(entityClass));
  }

  @Override
  public <R> Middleware<AsyncHandler<Response<R>>, AsyncHandler<Response<ByteString>>>
  asyncSerializerResponse(Class<? extends R> entityClass) {
    return inner -> inner.map(serialize(entityClass));
  }

  @Override
  public <E> Middleware<EntityHandler<E, ?>, SyncHandler<Response<ByteString>>>
  direct(Class<? extends E> entityClass) {
    return inner ->
        deserialize(entityClass)
            .flatMap(r -> ctx -> mapPayload(r, inner.invoke(ctx)))
            .map(serialize(entityClass));
  }

  @Override
  public <E> Middleware<EntityResponseHandler<E, ?>, SyncHandler<Response<ByteString>>>
  response(Class<? extends E> entityClass) {
    return inner ->
        deserialize(entityClass)
            .flatMap(r -> ctx -> flatMapPayload(r, inner.invoke(ctx)))
            .map(serialize(entityClass));
  }

  @Override
  public <E> Middleware<EntityAsyncHandler<E, ?>, AsyncHandler<Response<ByteString>>> asyncDirect(
      Class<? extends E> entityClass) {
    return inner ->
        Middleware.syncToAsync(deserialize(entityClass))
            .flatMap(r -> ctx -> asyncMapPayload(r, inner.invoke(ctx)))
            .map(serialize(entityClass));
  }

  @Override
  public <E> Middleware<EntityAsyncResponseHandler<E, ?>, AsyncHandler<Response<ByteString>>>
  asyncResponse(Class<? extends E> entityClass) {
    return inner ->
        Middleware.syncToAsync(deserialize(entityClass))
            .flatMap(r -> ctx -> asyncFlatMapPayload(r, inner.invoke(ctx)))
            .map(serialize(entityClass));
  }

  private <E> SyncHandler<Response<E>> deserialize(Class<? extends E> entityClass) {
    return requestContext -> {
      final Optional<ByteString> payloadOpt = requestContext.request().payload();
      if (!payloadOpt.isPresent()) {
        return Response.forStatus(
            Status.BAD_REQUEST
                .withReasonPhrase("Missing payload"));
      }

      final E entity;
      try {
        final ByteString byteString = payloadOpt.get();
        entity = codec.read(byteString.toByteArray(), entityClass);
      } catch (IOException e) {
        LOG.warn("error", e);
        return Response.forStatus(
            Status.BAD_REQUEST
                .withReasonPhrase("Payload parsing failed: " + e.getMessage()));
      }

      return Response.forPayload(entity);
    };
  }

  private <E> Function<Response<E>, Response<ByteString>> serialize(Class<? extends E> entityClass) {
    return response -> {
      final Optional<E> entityOpt = response.payload();

      if (!entityOpt.isPresent()) {
        //noinspection unchecked
        return (Response<ByteString>) response;
      }

      final ByteString bytes;
      try {
        bytes = ByteString.of(codec.write(entityOpt.get(), entityClass));
      } catch (IOException e) {
        LOG.error("error", e);
        return Response.forStatus(
            Status.INTERNAL_SERVER_ERROR
                .withReasonPhrase("Payload serialization failed: " + e.getMessage()));
      }

      return response.withPayload(bytes)
          .withHeader(CONTENT_TYPE, contentType);
    };
  }

  private <E> Response<E> ensureResponse(E response) {
    if (response instanceof Response) {
      //noinspection unchecked
      return (Response<E>) response;
    }

    return Response.forPayload(response);
  }

  private <E, R> Response<R> mapPayload(
      Response<E> in,
      Function<? super E, ? extends R> fn) {

    //noinspection unchecked
    return (in.payload().map(fn).isPresent())
        ? in.withPayload(in.payload().map(fn).get())
        : (Response<R>) in;
  }

  private <E, R> Response<R> flatMapPayload(
      Response<E> in,
      Function<? super E, ? extends Response<? extends R>> fn) {

    //noinspection unchecked
    return (in.payload().isPresent())
        ? (Response<R>) fn.apply(in.payload().get()).withHeaders(in.headers())
        : (Response<R>) in;
  }

  private <E, R> CompletionStage<Response<R>> asyncMapPayload(
      Response<E> in,
      Function<? super E, ? extends CompletionStage<? extends R>> fn) {

    //noinspection unchecked
    return (in.payload().isPresent())
        ? fn.apply(in.payload().get()).thenApply(in::withPayload)
        : completedFuture((Response<R>) in);
  }

  private <E, R> CompletionStage<Response<R>> asyncFlatMapPayload(
      Response<E> in,
      Function<? super E, ? extends CompletionStage<? extends Response<? extends R>>> fn) {

    //noinspection unchecked
    return (in.payload().isPresent())
        ? fn.apply(in.payload().get()).thenApply(ret -> (Response<R>) ret.withHeaders(in.headers()))
        : completedFuture((Response<R>) in);
  }
}
