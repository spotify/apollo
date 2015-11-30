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

import java.util.function.Function;

/**
 * Synchronous endpoint handler.
 *
 * Return a {@link Response} when you want to do things like modify response headers based on
 * results of service invocations or control the status code returned. Examples:
 *
 * <code>
 *   public Response<String> invoke(RequestContext requestContext) {
 *     String s = stringResponse();
 *     return Response.forPayload(s)
 *         .withHeader("X-Payload-Length", s.length());
 *   }
 * </code>
 *
 * <code>
 *   public Response<String> invoke(RequestContext requestContext) {
 *     Map<String, String> args = requestContext.pathArgs();
 *     if (args.get("arg") == null || args.get("arg").isEmpty()) {
 *       return Response.forStatus(Status.BAD_REQUEST
 *           .withReasonPhrase("Mandatory argument 'arg' missing"));
 *     }
 *
 *     return Response.forPayload(s);
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
public interface SyncHandler<T> {
  T invoke(RequestContext requestContext);

  /**
   * Create a new {@link SyncHandler} that will map the return value of
   * {@link #invoke(RequestContext)} through the given map function.
   *
   * @param mapFunction  The mapping function
   * @param <V>          The resulting handler type
   * @return A new {@link SyncHandler} with a composed invoke method
   */
  default <V> SyncHandler<V> map(Function<? super T, ? extends V> mapFunction) {
    return requestContext -> mapFunction.apply(invoke(requestContext));
  }

  /**
   * Create a new {@link SyncHandler} that will map the return value of
   * {@link #invoke(RequestContext)} through the given map function.
   *
   * The returned {@link SyncHandler} of the map function will execute with
   * the same {@link RequestContext} as the current handler.
   *
   * @param mapFunction  The mapping function
   * @param <V>          The resulting handler type
   * @return A new {@link SyncHandler} with a composed invoke method
   */
  default <V> SyncHandler<V> flatMap(Function<? super T, ? extends SyncHandler<? extends V>> mapFunction) {
    return requestContext -> mapFunction.apply(invoke(requestContext)).invoke(requestContext);
  }
}
