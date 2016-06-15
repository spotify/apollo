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
import com.typesafe.config.Config;

class EnvironmentFactoryImpl implements EnvironmentFactory {

  private final String backendDomain;
  private final Client client;
  private Resolver resolver;
  private final Closer closer;

  EnvironmentFactoryImpl(
      String backendDomain,
      Client client,
      Resolver resolver,
      Closer closer) {
    this.backendDomain = backendDomain;
    this.client = client;
    this.resolver = resolver;
    this.closer = closer;
  }

  @Override
  public Environment create(String serviceName, RoutingContext routingContext, Config config) {
    return new EnvironmentImpl(
        serviceName, backendDomain, client, resolver, routingContext, closer, config);
  }

  @Override
  public RoutingContext createRoutingContext() {
    return new RoutingContextImpl();
  }
}
