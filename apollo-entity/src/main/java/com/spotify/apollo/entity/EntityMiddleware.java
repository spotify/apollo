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
import com.spotify.apollo.route.AsyncHandler;
import com.spotify.apollo.route.Middleware;
import com.spotify.apollo.route.SyncHandler;

import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import okio.ByteString;

/**
 * Apollo {@link Middleware}s for route handlers that work with a typed entity.
 */
public interface EntityMiddleware {

  static EntityMiddleware forCodec(EntityCodec codec) {
    return new CodecEntityMiddleware(codec);
  }

  static EntityMiddleware forCodec(EntityCodec codec, String contentType) {
    return new CodecEntityMiddleware(codec, contentType);
  }

  <E> Middleware<EntityHandler<E, E>, SyncHandler<Response<ByteString>>>
  direct(Class<? extends E> requestEntityClass);

  <E, R> Middleware<EntityHandler<E, R>, SyncHandler<Response<ByteString>>>
  direct(Class<? extends E> requestEntityClass, Class<? extends R> responseEntityClass);

  <E> Middleware<EntityResponseHandler<E, E>, SyncHandler<Response<ByteString>>>
  response(Class<? extends E> requestEntityClass);

  <E, R> Middleware<EntityResponseHandler<E, R>, SyncHandler<Response<ByteString>>>
  response(Class<? extends E> requestEntityClass, Class<? extends R> responseEntityClass);

  <E> Middleware<EntityAsyncHandler<E, E>, AsyncHandler<Response<ByteString>>>
  asyncDirect(Class<? extends E> requestEntityClass);

  <E, R> Middleware<EntityAsyncHandler<E, R>, AsyncHandler<Response<ByteString>>>
  asyncDirect(Class<? extends E> requestEntityClass, Class<? extends R> responseEntityClass);

  <E> Middleware<EntityAsyncResponseHandler<E, E>, AsyncHandler<Response<ByteString>>>
  asyncResponse(Class<? extends E> requestEntityClass);

  <E, R> Middleware<EntityAsyncResponseHandler<E, R>, AsyncHandler<Response<ByteString>>>
  asyncResponse(Class<? extends E> requestEntityClass, Class<? extends R> responseEntityClass);

  <R> Middleware<SyncHandler<R>, SyncHandler<Response<ByteString>>>
  serializerDirect(Class<? extends R> responseEntityClass);

  <R> Middleware<SyncHandler<Response<R>>, SyncHandler<Response<ByteString>>>
  serializerResponse(Class<? extends R> responseEntityClass);

  <R> Middleware<AsyncHandler<R>, AsyncHandler<Response<ByteString>>>
  asyncSerializerDirect(Class<? extends R> responseEntityClass);

  <R> Middleware<AsyncHandler<Response<R>>, AsyncHandler<Response<ByteString>>>
  asyncSerializerResponse(Class<? extends R> responseEntityClass);

  /**
   * A common handler interface for the various entity handlers. This embodies the curried form
   * of the handlers so that the actual handler interfaces can have more readable definitions.
   *
   * @param <E>  The handler request entity type
   * @param <R>  The handler response entity type
   */
  @FunctionalInterface
  interface CurriedHandler<E, R>
      extends Function<RequestContext, Function<? super E, ? extends R>> {
  }

  @FunctionalInterface
  interface EntityHandler<E, R> extends CurriedHandler<E, R> {
    default EntityResponseHandler<E, R> asResponseHandler() {
      return rc -> e -> apply(rc).andThen(Response::forPayload).apply(e);
    }
  }

  @FunctionalInterface
  interface EntityResponseHandler<E, R> extends CurriedHandler<E, Response<R>> {
  }

  @FunctionalInterface
  interface EntityAsyncHandler<E, R> extends CurriedHandler<E, CompletionStage<R>> {
    default EntityAsyncResponseHandler<E, R> asResponseHandler() {
      return rc -> e -> apply(rc).andThen(s -> s.thenApply(Response::forPayload)).apply(e);
    }
  }

  @FunctionalInterface
  interface EntityAsyncResponseHandler<E, R>
      extends CurriedHandler<E, CompletionStage<Response<R>>> {
  }
}
