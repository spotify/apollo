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

import com.google.common.util.concurrent.FutureCallback;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static com.google.common.util.concurrent.Futures.addCallback;
import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * Defines a function that can be used to share functionality among routes using
 * {@link Route#with(Middleware, String, String, Object)}.
 * It also allows to compose several functions (middlewares).
 */
@FunctionalInterface
public interface Middleware<H, T> extends Function<H, T> {

  default <K> Middleware<H, K> and(Middleware<? super T, ? extends K> other) {
    return h -> other.apply(apply(h));
  }

  static <T> AsyncHandler<T> syncToAsync(SyncHandler<T> handler) {
    return requestContext -> completedFuture(handler.invoke(requestContext));
  }

  static <T> AsyncHandler<T> guavaToAsync(ListenableFutureHandler<T> listenableFutureHandler) {
    return requestContext -> {
      CompletableFuture<T> future = new CompletableFuture<>();

      addCallback(
          listenableFutureHandler.invoke(requestContext),
          new FutureCallback<T>() {
            @Override
            public void onSuccess(T result) {
              future.complete(result);
            }

            @Override
            public void onFailure(Throwable t) {
              future.completeExceptionally(t);
            }
          });

      return future;
    };
  }

}
