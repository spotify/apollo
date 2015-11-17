/*
 * -\-\-
 * Spotify Apollo API Implementations
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

import com.google.common.collect.ImmutableList;

import com.spotify.apollo.Response;
import com.spotify.apollo.route.AsyncHandler;
import com.spotify.apollo.route.Route;
import com.spotify.apollo.route.RouteProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Stream;

import okio.ByteString;

import static com.spotify.apollo.Environment.RoutingEngine;
import static com.spotify.apollo.environment.EnvironmentFactory.RoutingContext;
import static com.spotify.apollo.route.Middlewares.apolloDefaults;

class RoutingContextImpl implements RoutingContext {

  private static final Logger LOG = LoggerFactory.getLogger(RoutingContextImpl.class);

  private final ImmutableList.Builder<Object> builder = ImmutableList.builder();

  private volatile boolean built;

  @Override
  public synchronized Iterable<Object> endpointObjects() {
    built = true;
    return builder.build();
  }

  @Override
  public synchronized RoutingEngine registerRoutes(RouteProvider routeProvider) {
    ensureNotBuilt();

    routeProvider.routes()
        .map(route -> route.withMiddleware(apolloDefaults()))
        .forEach(this::addRoute);
    return this;
  }

  @Override
  public synchronized RoutingEngine registerRoute(Route<? extends AsyncHandler<?>> route) {
    ensureNotBuilt();

    addRoute(route.withMiddleware(apolloDefaults()));

    return this;
  }

  @Override
  public synchronized RoutingEngine registerSafeRoutes(
      Stream<? extends Route<? extends AsyncHandler<? extends Response<ByteString>>>> routes) {
    ensureNotBuilt();

    routes.forEach(this::addRoute);
    return this;
  }

  @Override
  public synchronized RoutingEngine registerSafeRoute(
      Route<? extends AsyncHandler<? extends Response<ByteString>>> route) {
    ensureNotBuilt();

    addRoute(route);

    return this;
  }

  private void addRoute(Route<? extends AsyncHandler<? extends Response<ByteString>>> route) {
    logRoute(route);
    builder.add(route);
  }

  private static void logRoute(Route<?> route) {
    LOG.info("Registering Route {} ({})", route.uri(), route.method());
  }

  private void ensureNotBuilt() {
    if (built) {
      throw new IllegalStateException(
          "Routing Engine has already been initialized. This is most likely a sign of "
          + "Environment.routingEngine() being used outside of application initialisation");
    }
  }
}
