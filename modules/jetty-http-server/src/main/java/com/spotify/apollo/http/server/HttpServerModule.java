/*
 * Copyright (c) 2014 Spotify AB
 */
package com.spotify.apollo.http.server;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.name.Names;

import com.spotify.apollo.core.Service;
import com.spotify.apollo.module.AbstractApolloModule;

import java.util.Objects;

import javax.inject.Singleton;

public class HttpServerModule extends AbstractApolloModule {

  private final Runnable onClose;

  private HttpServerModule(Runnable onClose) {
    this.onClose = Objects.requireNonNull(onClose);
  }

  public static HttpServerModule create() {
    return new HttpServerModule(() -> {});
  }

  @VisibleForTesting
  static HttpServerModule createForTest(Runnable onClose) {
    return new HttpServerModule(onClose);
  }

  @Override
  protected void configure() {
    bind(HttpServerConfig.class);
    bind(Runnable.class).annotatedWith(Names.named("http-server-on-close")).toInstance(onClose);
    bind(HttpServer.class).toProvider(HttpServerProvider.class).in(Singleton.class);
  }

  @Override
  public String getId() {
    return "http.server";
  }

  @Override
  public double getPriority() {
    // servers should load quite late
    return -128.0;
  }

  public static HttpServer server(Service.Instance instance) {
    return instance.resolve(HttpServer.class);
  }
}
