/**
 * Copyright © 2006-2012 Spotify AB
 */

package com.spotify.apollo.environment;

import com.spotify.apollo.Environment;

import static com.spotify.apollo.Environment.RoutingEngine;

/**
 * A factory that provides Apollo application environments.
 */
public interface EnvironmentFactory {

  /**
   * Create a new application environment.
   *
   * @param serviceName     The application name
   * @param routingContext  A routing context for the created {@link Environment}
   */
  Environment create(String serviceName, RoutingContext routingContext);

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
