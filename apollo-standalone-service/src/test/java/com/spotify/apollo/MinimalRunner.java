/*
 * Copyright (c) 2015 Spotify AB
 */

package com.spotify.apollo;

import com.spotify.apollo.route.Route;
import com.spotify.apollo.standalone.StandaloneService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class MinimalRunner {

  private static final Logger LOG = LoggerFactory.getLogger(MinimalRunner.class);

  /**
   * Typical entry point of a service
   *
   * @param args  Program arguments
   */
  public static void main(String[] args) throws Exception {
    StandaloneService.boot(MinimalRunner::app, "test", "run", "foo", "-Dhttp.server.port=8080");
  }

  public static void app(Environment env) {
    env.routingEngine()
        .registerRoute(Route.sync("GET", "/", (requestContext) -> "Hello World"));

    env.closer()
        .register(() -> LOG.info("Goodbye World"));
  }
}
