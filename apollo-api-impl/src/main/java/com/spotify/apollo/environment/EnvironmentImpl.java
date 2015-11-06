/**
 * Copyright (C) 2012 Spotify AB
 */

package com.spotify.apollo.environment;

import com.google.common.io.Closer;

import com.spotify.apollo.Client;
import com.spotify.apollo.Environment;
import com.spotify.apollo.environment.EnvironmentFactory.Resolver;
import com.typesafe.config.Config;

import static com.spotify.apollo.environment.EnvironmentFactory.RoutingContext;
import static java.util.Objects.requireNonNull;

/**
 * Provides an environment for Apollo applications.
 */
class EnvironmentImpl implements Environment {

  private final String serviceName;
  private final String domain;
  private final Client client;
  private final EnvironmentConfigResolver configResolver;
  private final Resolver resolver;
  private final RoutingContext routingContext;
  private final Closer closer;

  EnvironmentImpl(
      String serviceName,
      String domain,
      Client client,
      EnvironmentConfigResolver configResolver,
      Resolver resolver,
      RoutingContext routingContext,
      Closer closer) {
    this.serviceName = requireNonNull(serviceName, "serviceName");
    this.domain = requireNonNull(domain, "domain");
    this.client = requireNonNull(client, "client");
    this.configResolver = requireNonNull(configResolver, "configResolver");
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
  public Config config() {
    return configResolver.getConfig(serviceName);
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
