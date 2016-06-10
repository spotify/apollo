/*
 * -\-\-
 * Spotify Apollo API Implementations
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
package com.spotify.apollo.environment;

import com.google.common.io.Closer;

import com.spotify.apollo.Client;
import com.spotify.apollo.Environment;
import com.spotify.apollo.environment.EnvironmentFactory.Resolver;

import static com.spotify.apollo.environment.EnvironmentFactory.RoutingContext;
import static java.util.Objects.requireNonNull;

/**
 * Provides an environment for Apollo applications.
 */
class EnvironmentImpl implements Environment {

  private final String serviceName;
  private final String domain;
  private final Client client;
  private final Resolver resolver;
  private final RoutingContext routingContext;
  private final Closer closer;

  EnvironmentImpl(
      String serviceName,
      String domain,
      Client client,
      Resolver resolver,
      RoutingContext routingContext,
      Closer closer) {
    this.serviceName = requireNonNull(serviceName, "serviceName");
    this.domain = requireNonNull(domain, "domain");
    this.client = requireNonNull(client, "client");
    this.resolver = requireNonNull(resolver, "resolver");
    this.routingContext = requireNonNull(routingContext, "routingContext");
    this.closer = requireNonNull(closer, "closer");
  }

  @Override
  public String domain() {
    return domain;
  }

  @Override
  public Client client() {
    return client;
  }

  @Override
  public RoutingEngine routingEngine() {
    return routingContext;
  }

  @Override
  public Closer closer() {
    return closer;
  }

  @Override
  public <T> T resolve(Class<T> clazz) {
    return resolver.resolve(clazz);
  }
}
