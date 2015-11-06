/*
 * Copyright (c) 2014 Spotify AB
 */

package com.spotify.apollo.environment;

import com.google.common.io.Closer;

import com.spotify.apollo.Client;
import com.spotify.apollo.Environment;

class EnvironmentFactoryImpl implements EnvironmentFactory {

  private final String backendDomain;
  private final Client client;
  private final EnvironmentConfigResolver configResolver;
  private Resolver resolver;
  private final Closer closer;

  EnvironmentFactoryImpl(
      String backendDomain,
      Client client,
      EnvironmentConfigResolver configResolver,
      Resolver resolver,
      Closer closer) {
    this.backendDomain = backendDomain;
    this.client = client;
    this.configResolver = configResolver;
    this.resolver = resolver;
    this.closer = closer;
  }

  @Override
  public Environment create(String serviceName, RoutingContext routingContext) {
    return new EnvironmentImpl(
        serviceName, backendDomain, client, configResolver, resolver, routingContext, closer);
  }

  @Override
  public RoutingContext createRoutingContext() {
    return new RoutingContextImpl();
  }
}
