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

import com.spotify.apollo.Environment;
import com.typesafe.config.Config;

import static com.spotify.apollo.Environment.RoutingEngine;

/**
 * A factory that provides Apollo application environments.
 */
public interface EnvironmentFactory {

  /**
   * Create a new application environment.
   * @param serviceName     The application name
   * @param routingContext  A routing context for the created {@link Environment}
   * @param config          Service-specific configuration
   */
  Environment create(String serviceName, RoutingContext routingContext, Config config);

  /**
   * Creates a {@link RoutingContext} to be used with an {@link Environment}.
   *
   * @return A {@link RoutingContext} instance
   */
  RoutingContext createRoutingContext();

  /**
   * A {@link RoutingContext} is a context object for routing components set up
   * by the application. It extends the {@link RoutingEngine} interface
   * thus can be exposed through the {@link Environment}, but adds additional
   * methods to be used by a server framework.
   */
  interface RoutingContext extends RoutingEngine {
    Iterable<Object> endpointObjects();
  }

  @FunctionalInterface
  interface Resolver {
    <T> T resolve(Class<T> clazz);
  }
}
