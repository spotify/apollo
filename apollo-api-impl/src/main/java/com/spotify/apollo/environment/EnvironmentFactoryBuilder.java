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
import com.spotify.apollo.environment.EnvironmentFactory.Resolver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

public final class EnvironmentFactoryBuilder {

  private final String backendDomain;
  private final Client client;
  private final Closer closer;
  private final Resolver resolver;

  private final Optional<EnvironmentConfigResolver> configResolver;

  EnvironmentFactoryBuilder(String backendDomain, Client client, Closer closer, Resolver resolver) {
    this(backendDomain, client, closer, resolver, Optional.empty());
  }

  EnvironmentFactoryBuilder(
      String backendDomain, Client client, Closer closer, Resolver resolver,
      Optional<EnvironmentConfigResolver> configResolver) {
    this.backendDomain = requireNonNull(backendDomain, "backendDomain");
    this.client = requireNonNull(client, "client");
    this.closer = requireNonNull(closer, "closer");
    this.resolver = requireNonNull(resolver, "resolver");
    this.configResolver = requireNonNull(configResolver, "configResolver");
  }

  /**
   * Use custom config resolver.
   *
   * Only one of
   * {@link #withConfigResolver(EnvironmentConfigResolver)},
   * {@link #withStaticConfig(Config)} and
   * {@link #withClassLoader(ClassLoader)}
   * can be used per builder.
   *
   * @param configResolver  The config resolver to use.
   */
  public EnvironmentFactoryBuilder withConfigResolver(EnvironmentConfigResolver configResolver) {
    checkState(!this.configResolver.isPresent(), "Configuration resolution already set");

    return new EnvironmentFactoryBuilder(backendDomain, client, closer, resolver,
                                         Optional.of(configResolver));
  }

  /**
   * Statically inject a config object into the environment.
   *
   * Only one of
   * {@link #withConfigResolver(EnvironmentConfigResolver)},
   * {@link #withStaticConfig(Config)} and
   * {@link #withClassLoader(ClassLoader)}
   * can be used per builder.
   *
   * @param configNode  The config object to use.
   */
//  public EnvironmentFactoryBuilder withStaticConfig(Config configNode) {
//    checkState(!this.configResolver.isPresent(), "Configuration resolution already set");
//
//    return new EnvironmentFactoryBuilder(backendDomain, client, closer, resolver,
//                                         Optional.of(new StaticConfigResolver(configNode)));
//  }

  /**
   * Lazily load configuration from this classloader.
   *
   * Only one of
   * {@link #withConfigResolver(EnvironmentConfigResolver)},
   * {@link #withStaticConfig(Config)} and
   * {@link #withClassLoader(ClassLoader)}
   * can be used per builder.
   *
   * @param classLoader  The class loader to lazily load configuration from.
   */
  public EnvironmentFactoryBuilder withClassLoader(ClassLoader classLoader) {
    checkState(!this.configResolver.isPresent(), "Configuration resolution already set");

    return new EnvironmentFactoryBuilder(backendDomain, client, closer, resolver,
                                         Optional.of(new LazyConfigResolver(classLoader)));
  }

  public EnvironmentFactory build() {
    EnvironmentConfigResolver configResolver = this.configResolver.isPresent()
        ? this.configResolver.get()
        : new LazyConfigResolver();

    return new EnvironmentFactoryImpl(backendDomain, client, configResolver, resolver, closer);
  }

  public static EnvironmentFactoryBuilder newBuilder(
      String backendDomain, Client client, Closer closer, Resolver resolver) {
    return new EnvironmentFactoryBuilder(backendDomain, client, closer, resolver);
  }

//  static class StaticConfigResolver implements EnvironmentConfigResolver {
//
//    private final Config configNode;
//
//    StaticConfigResolver(Config configNode) {
//      this.configNode = configNode;
//    }
//
//    @Override
//    public Config getConfig(String ignored) {
//      return configNode;
//    }
//  }

  static class LazyConfigResolver implements EnvironmentConfigResolver {

    private final Logger LOG = LoggerFactory.getLogger(LazyConfigResolver.class);

    private final Optional<ClassLoader> configClassLoader;

//    private Config configNode;

    LazyConfigResolver() {
      this.configClassLoader = Optional.empty();
    }

    LazyConfigResolver(ClassLoader configClassLoader) {
      this.configClassLoader = Optional.of(configClassLoader);
    }

//    @Override
//    public synchronized Config getConfig(String serviceName) {
//      // Lazy initialization here
//      if (configNode == null) {
//        // Bundled config is optional
//        if (configClassLoader.isPresent()) {
//          configNode = ConfigFactory.load(configClassLoader.get(), serviceName);
//        } else {
//          configNode = ConfigFactory.load(serviceName);
//        }
//      }
//      return configNode;
//    }
  }
}
