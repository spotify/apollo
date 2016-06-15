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
  private final JettyHttpServerConfiguration config;
  private final Runnable onClose;

  @Inject
  HttpServerProvider(
      Closer closer,
      JettyHttpServerConfiguration config,
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

  static boolean enabled(JettyHttpServerConfiguration config) {
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
