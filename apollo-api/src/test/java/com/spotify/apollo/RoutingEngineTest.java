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
package com.spotify.apollo;

import com.spotify.apollo.Environment.RoutingEngine;
import com.spotify.apollo.route.AsyncHandler;
import com.spotify.apollo.route.Route;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;

import okio.ByteString;

/**
 * A Test that mainly validates some wildcards in the signatures of {@link RoutingEngine}
 */
@RunWith(MockitoJUnitRunner.class)
public class RoutingEngineTest {

  @Mock RoutingEngine routingEngine;

  @Test
  public void shouldCompile() throws Exception {
    routingEngine.registerSafeRoute(Route.async("GET", "/foo", c -> foo()));
    routingEngine.registerSafeRoute(Route.async("GET", "/bar", c -> bar()));
    routingEngine.registerSafeRoute(route(c -> foo()));
    routingEngine.registerSafeRoute(route(c -> bar()));
    routingEngine.registerSafeRoutes(
        Stream.of(
            Route.async("GET", "/foo", c -> foo()),
            Route.async("GET", "/bar", c -> bar()),
            route(c -> foo()),
            route(c -> bar())
        ));
  }

  // A few dummy handlers and route builders
  static CompletionStage<MyResponse1> foo() {
    return null;
  }

  static CompletionStage<MyResponse2> bar() {
    return null;
  }

  static <H> MyRoute<MyHandler<H>> route(MyHandler<H> handler) {
    return null;
  }

  // A few dummy implementations of Route, AsyncHandler and Response used in this test
  interface MyRoute<H> extends Route<H> {
  }

  interface MyHandler<T> extends AsyncHandler<T> {
  }

  interface MyResponse2 extends Response<ByteString> {
  }

  interface MyResponse1 extends Response<ByteString> {
  }
}
