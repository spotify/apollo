/*
 * -\-\-
 * Spotify Apollo Testing Helpers
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
package com.spotify.apollo.test;

import com.spotify.apollo.Request;
import com.spotify.apollo.Response;
import com.spotify.apollo.environment.IncomingRequestAwareClient;
import com.spotify.apollo.test.StubClient.NoMatchingResponseFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import okio.ByteString;

final class FallbackClient implements IncomingRequestAwareClient {

  private static final Logger LOG = LoggerFactory.getLogger(FallbackClient.class);

  private final IncomingRequestAwareClient mainClient;
  private final IncomingRequestAwareClient fallbackClient;

  FallbackClient(IncomingRequestAwareClient mainClient, IncomingRequestAwareClient fallbackClient) {
    this.mainClient = mainClient;
    this.fallbackClient = fallbackClient;
  }

  @Override
  public CompletionStage<Response<ByteString>> send(Request request, Optional<Request> incoming) {
    final CompletionStage<Response<ByteString>> send = mainClient.send(request, incoming);
    return fallbackCompose(
        send,
        t -> {
          if (NoMatchingResponseFoundException.class.isAssignableFrom(t.getClass())) {
            NoMatchingResponseFoundException e = (NoMatchingResponseFoundException) t;
            LOG.debug("Falling back to real client: {}", e.getMessage());
            return fallbackClient.send(request, incoming);
          } else {
            return send;
          }
        });
  }

  private static <U, T extends U> CompletionStage<U> fallbackCompose(
      CompletionStage<T> future, Function<Throwable, ? extends CompletionStage<U>> fn) {

    final CompletableFuture<U> topFuture = new CompletableFuture<>();

    future.whenComplete(
        (r1, t1) -> {
          if (t1 != null) {
            fn.apply(t1).whenComplete(
                (r2, t2) -> {
                  if (t2 != null) {
                    topFuture.completeExceptionally(t2);
                  }
                  if (r2 != null) {
                    topFuture.complete(r2);
                  }
                }
            );
          }
          if (r1 != null) {
            topFuture.complete(r1);
          }
        }
    );

    return topFuture;
  }
}
