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
import com.google.inject.name.Named;

import com.spotify.apollo.Client;
import com.spotify.apollo.Environment;
import com.spotify.apollo.core.Services;
import com.spotify.apollo.environment.EnvironmentFactory.RoutingContext;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Singleton;

import static com.spotify.apollo.environment.ApolloEnvironmentModule.foldDecorators;
import static java.util.Objects.requireNonNull;

/**
 * Module supplying:
 *   {@link IncomingRequestAwareClient},
 *   {@link EnvironmentFactory},
 *   {@link RoutingContext},
 *   {@link Environment}
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
class EnvironmentModule extends AbstractModule {

  private final Comparator<ClientDecorator.Id> clientDecoratorComparator;

  private EnvironmentModule(Comparator<ClientDecorator.Id> clientDecoratorComparator) {
    this.clientDecoratorComparator = requireNonNull(clientDecoratorComparator);
  }


  @Provides
  @Singleton
  IncomingRequestAwareClient incomingRequestAwareClient(
      @Named(Services.INJECT_SERVICE_NAME) String serviceName,
      Set<ClientDecorator> clientDecorators) {

    IncomingRequestAwareClient clientStack = foldDecorators(new NoopClient(),
                                                            sort(clientDecorators, clientDecoratorComparator));
    return new ServiceSettingClient(serviceName, clientStack);
  }

  private List<ClientDecorator> sort(Set<ClientDecorator> clientDecorators,
                                     Comparator<ClientDecorator.Id> comparator) {
    // '.reversed', since when folding, the first decorator will be the innermost one. we want to
    // ensure that the first decorator according to the comparator is the outermost one.
    Comparator<ClientDecorator.Id> reversed = comparator.reversed();

    return clientDecorators.stream()
        .sorted((left, right) -> reversed.compare(left.id(), right.id()))
        .collect(Collectors.toList());
  }

  @Provides
  @Singleton
  EnvironmentFactory environmentFactory(
      ApolloConfig apolloConfig,
      Closer closer,
      Injector injector,
      IncomingRequestAwareClient incomingRequestAwareClient) {

    final String backend = apolloConfig.backend();
    final Client unawareClient = incomingRequestAwareClient.asUnawareClient();

    return new EnvironmentFactoryImpl(backend, unawareClient, injector::getInstance, closer);
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

  public static EnvironmentModule create(Comparator<ClientDecorator.Id> clientDecoratorComparator) {
    return new EnvironmentModule(clientDecoratorComparator);
  }

  @Override
  protected void configure() {

  }
}
