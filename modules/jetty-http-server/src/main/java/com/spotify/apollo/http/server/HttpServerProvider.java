/*
 * Copyright (c) 2014 Spotify AB
 */
package com.spotify.apollo.http.server;

import com.google.common.io.Closer;
import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.inject.Named;
import javax.inject.Provider;

class HttpServerProvider implements Provider<HttpServer> {

  private static final Logger LOG = LoggerFactory.getLogger(HttpServerProvider.class);

  private final Closer closer;
  private final HttpServerConfig config;
  private final Runnable onClose;

  @Inject
  HttpServerProvider(
      Closer closer,
      HttpServerConfig config,
      @Named("http-server-on-close") Runnable onClose) {
    this.closer = closer;
    this.config = config;
    this.onClose = onClose;
  }

  @Override
  public HttpServer get() {
    if (!enabled(config)) {
      return NoopServer.INSTANCE;
    } else {
      return new HttpServerImpl(closer, config, onClose);
    }
  }

  static boolean enabled(HttpServerConfig config) {
    return config.port() != null;
  }

  private enum NoopServer implements HttpServer {
    INSTANCE;

    @Override
    public void start(com.spotify.apollo.request.RequestHandler requestHandler) {
      LOG.warn("Not loading http server - enable with: http.server.port = 8080");
    }

    @Override
    public void close() throws IOException {
      // Do nothing
    }
  }
}
