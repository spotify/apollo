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

import com.spotify.apollo.module.AbstractApolloModule;
import com.spotify.apollo.request.RequestRunnableFactory;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

/**
 * A module used for testing. It counts and asserts requests against a certain uri.
 */
class RequestInspectingModule extends AbstractApolloModule
    implements RequestRunnableFactoryDecorator {

  private String matchUri;
  private final AtomicInteger requestCounter;

  RequestInspectingModule(String matchUri, AtomicInteger requestCounter) {
    this.matchUri = matchUri;
    this.requestCounter = requestCounter;
  }

  @Override
  protected void configure() {
    Multibinder.newSetBinder(binder(), RequestRunnableFactoryDecorator.class)
        .addBinding().toInstance(this);
  }

  @Override
  public String getId() {
    return "request-inspector";
  }

  @Override
  public RequestRunnableFactory apply(RequestRunnableFactory requestRunnableFactory) {
    return request -> {
      assertEquals(matchUri, request.request().uri());
      requestCounter.incrementAndGet();
      return requestRunnableFactory.create(request);
    };
  }
}
