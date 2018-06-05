/*
 * -\-\-
 * Spotify Apollo Jetty HTTP Server Module
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
package com.spotify.apollo.http.server;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.Closer;

import com.spotify.apollo.RequestMetadata;
import com.spotify.apollo.request.RequestHandler;
import com.spotify.apollo.request.RequestMetadataImpl;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.time.Duration;

/**
 * A fully configured server that can be started and stopped.
 */
class HttpServerImpl implements HttpServer {

  private static final Logger LOG = LoggerFactory.getLogger(HttpServer.class);

  private final Closer closer;
  private final HttpServerConfig config;
  private final Runnable onClose;
  private final RequestOutcomeConsumer logger;

  @VisibleForTesting
  Server server;

  HttpServerImpl(Closer closer, HttpServerConfig config, Runnable onClose,
                 RequestOutcomeConsumer logger) {
    this.closer = closer;
    this.config = config;
    this.onClose = onClose;
    this.logger = logger;
  }

  @Override
  public void start(RequestHandler requestHandler) {
    LOG.info("Starting Jetty HTTP server on {}:{}", config.address(), config.port());
    final InetSocketAddress serverSocketAddress =
        new InetSocketAddress(config.address(), config.port());
    final RequestMetadata.HostAndPort serverInfo =
        RequestMetadataImpl.hostAndPort(config.address(), config.port());

    server = new Server(serverSocketAddress);
    ((QueuedThreadPool) server.getThreadPool()).setMaxThreads(config.maxThreads());

    server.setHandler(new ApolloRequestHandler(serverInfo, requestHandler,
                                               Duration.ofMillis(config.ttlMillis()), logger));
    try {
      server.start();
      closer.register(this);
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
