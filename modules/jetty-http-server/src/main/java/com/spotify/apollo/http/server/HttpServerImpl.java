/*
 * Copyright (c) 2015 Spotify AB
 */

package com.spotify.apollo.http.server;

import com.google.common.io.Closer;

import com.spotify.apollo.request.RequestHandler;
import com.spotify.apollo.request.ServerInfo;
import com.spotify.apollo.request.ServerInfos;

import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

/**
 * A fully configured server that can be started and stopped.
 */
class HttpServerImpl implements HttpServer {

  private static final Logger LOG = LoggerFactory.getLogger(HttpServer.class);
  private static final String SERVER_ID = "http";

  private final Closer closer;
  private final HttpServerConfig config;
  private final Runnable onClose;

  private Server server;

  HttpServerImpl(Closer closer, HttpServerConfig config, Runnable onClose) {
    this.closer = closer;
    this.config = config;
    this.onClose = onClose;
  }

  @Override
  public void start(RequestHandler requestHandler) {
    LOG.info("Starting Jetty HTTP server on {}:{}", config.address(), config.port());
    final InetSocketAddress serverSocketAddress =
        new InetSocketAddress(config.address(), config.port());
    final ServerInfo serverInfo =
        ServerInfos.create(SERVER_ID, serverSocketAddress);

    server = new Server(serverSocketAddress);
    server.setHandler(new ApolloRequestHandler(serverInfo, requestHandler));
    try {
      server.start();
      closer.register(this::close);
    } catch (Exception e) {
      throw new RuntimeException("Failed to start server", e);
    }
  }

  @Override
  public void close() {
    try {
      server.stop();
      if (onClose != null) {
        onClose.run();
      }
    } catch (Exception e) {
      LOG.warn("Could not close jetty http server", e);
    }
  }
}
