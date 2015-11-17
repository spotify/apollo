/*
 * -\-\-
 * Spotify Apollo API Environment
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
package com.spotify.apollo.environment;

import com.google.inject.multibindings.Multibinder;

import com.spotify.apollo.Response;
import com.spotify.apollo.module.AbstractApolloModule;
import com.spotify.apollo.request.EndpointRunnableFactory;

import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import okio.ByteString;

import static com.spotify.apollo.Response.forStatus;
import static com.spotify.apollo.Status.GONE;

/**
 * A module used for testing. It remembers the last response of any matching endpoint as a String
 * and always replies to the incoming request with an empty GONE status.
 */
class LastResponseModule extends AbstractApolloModule
    implements EndpointRunnableFactoryDecorator {

  private final AtomicReference<String> lastResponseRef;

  LastResponseModule(AtomicReference<String> lastResponseRef) {
    this.lastResponseRef = lastResponseRef;
  }

  @Override
  protected void configure() {
    Multibinder.newSetBinder(binder(), EndpointRunnableFactoryDecorator.class)
        .addBinding().toInstance(this);
  }

  @Override
  public String getId() {
    return "other";
  }

  @Override
  public EndpointRunnableFactory apply(EndpointRunnableFactory ignored) {
    return (request, requestContext, endpoint) -> {

      // we'll intercept and do the endpoint call
      Response<ByteString> response = getUnchecked(endpoint.invoke(requestContext));
      Optional<ByteString> payload = response.payload();
      if (payload.isPresent()) {
        lastResponseRef.set(payload.get().utf8());
      }

      // return a GONE response
      return () -> request.reply(forStatus(GONE));
    };
  }

  private static <T> T getUnchecked(CompletionStage<T> stage) {
    try {
      return stage.toCompletableFuture().get();
    } catch (ExecutionException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
