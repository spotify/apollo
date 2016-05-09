/*
 * -\-\-
 * Spotify Apollo API Environment
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
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.OptionalBinder;
import com.google.inject.name.Named;

import com.spotify.apollo.Client;
import com.spotify.apollo.Environment;
import com.spotify.apollo.core.Services;
import com.spotify.apollo.environment.EnvironmentFactory.RoutingContext;
import com.typesafe.config.Config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.inject.Singleton;

import static com.spotify.apollo.environment.ApolloEnvironmentModule.foldDecorators;

/**
 * Module supplying:
 *   {@link IncomingRequestAwareClient},
 *   {@link EnvironmentFactory},
 *   {@link RoutingContext},
 *   {@link Environment}
 */
class EnvironmentModule extends AbstractModule {
  @Provides
  @Singleton
  IncomingRequestAwareClient incomingRequestAwareClient(
      @Named(Services.INJECT_SERVICE_NAME) String serviceName,
      Set<ClientDecorator> clientDecorators,
      @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
          Optional<Comparator<ClientDecorator>> clientDecoratorComparator) {

    List<ClientDecorator> sortedDecorators = new ArrayList<>(clientDecorators);
    clientDecoratorComparator
        .ifPresent(comparator -> Collections.sort(sortedDecorators, comparator));

    final IncomingRequestAwareClient clientStack =
        foldDecorators(new NoopClient(), sortedDecorators);
    return new ServiceSettingClient(serviceName, clientStack);
  }

  @Provides
  @Singleton
  EnvironmentFactory environmentFactory(
      Config configNode,
      ApolloConfig apolloConfig,
      Closer closer,
      Injector injector,
      IncomingRequestAwareClient incomingRequestAwareClient) {

    final String backend = apolloConfig.backend();
    final Client unawareClient = incomingRequestAwareClient.asUnawareClient();

    return EnvironmentFactoryBuilder.newBuilder(backend, unawareClient, closer, injector::getInstance)
        .withStaticConfig(configNode)
        .build();
  }

  @Provides
  @Singleton
  RoutingContext routingContext(EnvironmentFactory environmentFactory) {
    return environmentFactory.createRoutingContext();
  }

  @Provides
  @Singleton
  Environment environment(
      @Named(Services.INJECT_SERVICE_NAME) String serviceName,
      EnvironmentFactory environmentFactory,
      RoutingContext routingContext) {
    return environmentFactory.create(serviceName, routingContext);
  }

  @Override
  protected void configure() {
    OptionalBinder.newOptionalBinder(binder(), new TypeLiteral<Comparator<ClientDecorator>>() {});
  }
}
