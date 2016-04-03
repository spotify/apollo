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

import com.spotify.apollo.RequestContext;
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

import javaslang.control.Either;
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
  serializerDirect(Class<? extends R> entityResponseClass) {
    return inner -> inner
        .map(Response::forPayload)
        .map(serialize(entityResponseClass));
  }

  @Override
  public <R> Middleware<SyncHandler<Response<R>>, SyncHandler<Response<ByteString>>>
  serializerResponse(Class<? extends R> entityResponseClass) {
    return inner -> inner
        .map(serialize(entityResponseClass));
  }

  @Override
  public <R> Middleware<AsyncHandler<R>, AsyncHandler<Response<ByteString>>>
  asyncSerializerDirect(Class<? extends R> entityResponseClass) {
    return inner -> inner
        .map(Response::forPayload)
        .map(serialize(entityResponseClass));
  }

  @Override
  public <R> Middleware<AsyncHandler<Response<R>>, AsyncHandler<Response<ByteString>>>
  asyncSerializerResponse(Class<? extends R> entityResponseClass) {
    return inner -> inner
        .map(serialize(entityResponseClass));
  }

  @Override
  public <E> Middleware<EntityHandler<E, E>, SyncHandler<Response<ByteString>>>
  direct(Class<? extends E> entityClass) {
    return direct(entityClass, entityClass);
  }

  @Override
  public <E, R> Middleware<EntityHandler<E, R>, SyncHandler<Response<ByteString>>>
  direct(Class<? extends E> entityClass, Class<? extends R> entityResponseClass) {
    return inner -> withEntity(inner.asResponseHandler(), entityClass)
        .map(serialize(entityResponseClass));
  }

  @Override
  public <E> Middleware<EntityResponseHandler<E, E>, SyncHandler<Response<ByteString>>>
  response(Class<? extends E> entityClass) {
    return response(entityClass, entityClass);
  }

  @Override
  public <E, R> Middleware<EntityResponseHandler<E, R>, SyncHandler<Response<ByteString>>>
  response(Class<? extends E> entityClass, Class<? extends R> entityResponseClass) {
    return inner -> withEntity(inner, entityClass)
        .map(serialize(entityResponseClass));
  }

  @Override
  public <E> Middleware<EntityAsyncHandler<E, E>, AsyncHandler<Response<ByteString>>>
  asyncDirect(Class<? extends E> entityClass) {
    return asyncDirect(entityClass, entityClass);
  }

  @Override
  public <E, R> Middleware<EntityAsyncHandler<E, R>, AsyncHandler<Response<ByteString>>>
  asyncDirect(Class<? extends E> entityClass, Class<? extends R> entityResponseClass) {
    return inner -> withEntityAsync(inner.asResponseHandler(), entityClass)
        .map(serialize(entityResponseClass));
  }

  @Override
  public <E> Middleware<EntityAsyncResponseHandler<E, E>, AsyncHandler<Response<ByteString>>>
  asyncResponse(Class<? extends E> entityClass) {
    return asyncResponse(entityClass, entityClass);
  }

  @Override
  public <E, R> Middleware<EntityAsyncResponseHandler<E, R>, AsyncHandler<Response<ByteString>>>
  asyncResponse(Class<? extends E> entityClass, Class<? extends R> entityResponseClass) {
    return inner -> withEntityAsync(inner, entityClass)
        .map(serialize(entityResponseClass));
  }

  private <E, R> SyncHandler<Response<R>> withEntity(
      EntityResponseHandler<E, R> inner,
      Class<? extends E> entityClass) {
    //noinspection unchecked
    return rc -> deserialize(rc, entityClass)
        .map(inner.invoke(rc))
        .getOrElseGet(left -> (Response<R>) left);
  }

  private <E, R> AsyncHandler<Response<R>> withEntityAsync(
      EntityAsyncResponseHandler<E, R> inner,
      Class<? extends E> entityClass) {
    //noinspection unchecked
    return rc -> deserialize(rc, entityClass)
        .map(inner.invoke(rc))
        .getOrElseGet(left -> completedFuture((Response<R>) left));
  }

  private <E> Either<Response<?>, E> deserialize(RequestContext rc, Class<? extends E> entityClass) {
    final Optional<ByteString> payloadOpt = rc.request().payload();
    if (!payloadOpt.isPresent()) {
      return Either.left(Response.forStatus(
          Status.BAD_REQUEST
              .withReasonPhrase("Missing payload")));
    }

    final E entity;
    try {
      final ByteString byteString = payloadOpt.get();
      entity = codec.read(byteString.toByteArray(), entityClass);
    } catch (IOException e) {
      LOG.warn("error", e);
      return Either.left(Response.forStatus(
          Status.BAD_REQUEST
              .withReasonPhrase("Payload parsing failed: " + e.getMessage())));
    }

    return Either.right(entity);
  }

  private <R> Function<Response<R>, Response<ByteString>> serialize(Class<? extends R> entityClass) {
    return response -> {
      final Optional<R> entityOpt = response.payload();

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
}
