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
import com.google.inject.Singleton;
import com.google.inject.name.Names;

import com.spotify.apollo.core.Service;
import com.spotify.apollo.module.AbstractApolloModule;

import java.util.Objects;

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
