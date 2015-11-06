/**
 * Copyright © 2006-2012 Spotify AB
 */

package com.spotify.apollo;

import com.google.common.io.Closer;

import com.spotify.apollo.route.AsyncHandler;
import com.spotify.apollo.route.Middleware;
import com.spotify.apollo.route.Route;
import com.spotify.apollo.route.RouteProvider;
import com.typesafe.config.Config;

import java.util.stream.Stream;

import okio.ByteString;

/**
 * An interface that an application can use to interact with the execution
 * environment of an Apollo application.
 */
public interface Environment {

  /**
   * The domain that the application is running in.
   *
   * @return The domain name. E.g. shared.cloud, sto3, etc.
   */
  String domain();

  /**
   * Get an Apollo client for communicating with a backend service.
   *
   * Within request handling, use {@link RequestContext#requestScopedClient()}
   * for Apollo to be able to connect outgoing requests with incoming ones, and
   * to set the auth context of outgoing requests based on the incoming one.
   *
   * @return A {@link Client}.
   */
  Client client();

  /**
   * Returns configuration loaded by the framework on an application behalf.
   *
   * @return loaded configuration node.
   */
  Config config();

  /**
   * Returns the {@link RoutingEngine} of this application.
   *
   * @return The {@link RoutingEngine} instance of this application
   */
  RoutingEngine routingEngine();

  /**
   * Returns a {@link Closer} which can be used to register resources that need to be closed on
   * application shutdown.
   *
   * @return the {@link Closer} of this application
   */
  Closer closer();

  /**
   * Resolves an instance of a class out of the underlying apollo-core module system.
   *
   * @param clazz  The class to resolve
   * @param <T>    The type of the resoved instance
   * @return An instance of the resolved class
   */
  <T> T resolve(Class<T> clazz);

  interface RoutingEngine {

    /**
     * Registers a {@link RouteProvider}. This is a convenience method; using it means that Apollo
     * will internally apply its default middlewares to the handlers for each route that
     * ensure unserialized responses are serialized if possible, and that HTTP semantics
     * regarding when to return payloads and set Content-Length headers are respected.
     *
     * This method is convenient but loses type safety due to the way serialization is done. It may
     * be better to use the {@link #registerSafeRoutes(Stream)} method instead, writing a
     * {@link Middleware} that does serialization in a type-safe way.
     *
     * @param routeProvider The {@link RouteProvider} to register.
     */
    RoutingEngine registerRoutes(RouteProvider routeProvider);

    /**
     * Registers a {@link Route}.
     *
     * @param route The {@link Route} to register.
     */
    RoutingEngine registerRoute(Route<? extends AsyncHandler<?>> route);

    /**
     * Registers routes. Apollo will not apply any further Middlewares to the routes.
     *
     * @param routes The {@link Stream} of {@link Route}s to register.
     */
    RoutingEngine registerSafeRoutes(
        Stream<? extends Route<? extends AsyncHandler<? extends Response<ByteString>>>> routes);

    /**
     * Registers a {@link Route}. Apollo will not apply any further Middlewares to the route.
     *
     * @param route The {@link Route} to register.
     */
    RoutingEngine registerSafeRoute(
        Route<? extends AsyncHandler<? extends Response<ByteString>>> route);
  }
}
