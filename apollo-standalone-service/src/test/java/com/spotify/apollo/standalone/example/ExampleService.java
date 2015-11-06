/*
 * Copyright (c) 2014 Spotify AB
 */
package com.spotify.apollo.standalone.example;

import com.spotify.apollo.Environment;
import com.spotify.apollo.core.Service;
import com.spotify.apollo.route.Route;
import com.spotify.apollo.standalone.LoadingException;
import com.spotify.apollo.standalone.StandaloneService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ExampleService {

  private static final Logger LOG = LoggerFactory.getLogger(ExampleService.class);

  public static void main(String[] args) throws LoadingException {

    final Service service = StandaloneService.usingAppInit(ExampleService::init, "ping")
        .build();

    StandaloneService.boot(service, args);
    LOG.info("bye bye");
  }

  public static void init(Environment environment) {
    environment.routingEngine()
        .registerRoute(Route.sync("GET", "/hello", c -> "hello world"));
    LOG.info("in app init {}", environment);
  }
}
