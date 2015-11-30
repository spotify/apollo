/*
 * -\-\-
 * Spotify Apollo API Interfaces
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

import com.spotify.apollo.Environment.RoutingEngine;
import com.spotify.apollo.RequestContext;
import com.spotify.apollo.Response;

import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/**
 * Asynchronous endpoint handler.
 *
 * Return a stage with a {@link Response} value when you want to do things like modify response
 * headers based on results of service invocations or if you want to set the status code. Example:
 *
 * <code>
 *   public CompletionStage<Response<String>> invoke(RequestContext requestContest) {
 *     return futureToStringPayload().thenApply(
 *         s -> Response.forPayload(s)
 *                .withHeader("X-Payload-Length", s.length()));
 *   }
 * </code>
 *
 * In order to construct a {@link Route} that can be registered with
 * {@link RoutingEngine#registerRoute(Route)}, the return type of the handler must be a
 * {@code Response<ByteString>}. This return type can be composed through your serialization
 * functions using {@link #map(Function)} and {@link #flatMap(Function)}.
 *
 * @param <T>  The return type of the handler
 */
@FunctionalInterface
public interface AsyncHandler<T> {
  CompletionStage<T> invoke(RequestContext requestContext);

  /**
   * Create a new {@link AsyncHandler} that will map the return value of
   * {@link #invoke(RequestContext)} through the given map function.
   *
   * @param mapFunction  The mapping function
   * @param <V>          The resulting handler type
   * @return A new {@link AsyncHandler} with a composed invoke method
   */
  default <V> AsyncHandler<V> map(Function<? super T, ? extends V> mapFunction) {
    return requestContext -> invoke(requestContext).thenApply(mapFunction);
  }

  /**
   * Create a new {@link AsyncHandler} that will map the return value of
   * {@link #invoke(RequestContext)} through the given map function.
   *
   * The returned {@link AsyncHandler} of the map function will execute with
   * the same {@link RequestContext} as the current handler.
   *
   * @param mapFunction  The mapping function
   * @param <V>          The resulting handler type
   * @return A new {@link AsyncHandler} with a composed invoke method
   */
  default <V> AsyncHandler<V> flatMap(Function<? super T, ? extends AsyncHandler<? extends V>> mapFunction) {
    //noinspection unchecked
    return requestContext -> invoke(requestContext)
        .thenCompose(t -> (CompletionStage<V>) mapFunction.apply(t).invoke(requestContext));
  }

  /**
   * Synchronous version of {@link #flatMap(Function)}. Use this when you want to do
   * synchronous maps of the handler return value.
   *
   * @param mapFunction  The mapping function
   * @param <V>          The resulting handler type
   * @return A new {@link AsyncHandler} with a composed invoke method
   */
  default <V> AsyncHandler<V> flatMapSync(Function<? super T, ? extends SyncHandler<? extends V>> mapFunction) {
    return requestContext -> invoke(requestContext)
        .thenApply(t -> mapFunction.apply(t).invoke(requestContext));
  }
}
