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

import com.google.common.collect.Iterables;
import com.google.common.io.Closer;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;

import com.spotify.apollo.AppInit;
import com.spotify.apollo.Client;
import com.spotify.apollo.Environment;
import com.spotify.apollo.core.Service;
import com.spotify.apollo.dispatch.Endpoint;
import com.spotify.apollo.meta.ApplicationOrMetaRouter;
import com.spotify.apollo.meta.MetaApplication;
import com.spotify.apollo.meta.MetaInfoTracker;
import com.spotify.apollo.meta.model.MetaGatherer;
import com.spotify.apollo.module.AbstractApolloModule;
import com.spotify.apollo.request.EndpointRunnableFactory;
import com.spotify.apollo.request.RequestHandler;
import com.spotify.apollo.request.RequestRunnableFactory;
import com.spotify.apollo.route.ApplicationRouter;
import com.spotify.apollo.route.Routers;
import com.typesafe.config.Config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.function.Function;

import static com.spotify.apollo.request.Handlers.endpointRunnableFactory;
import static com.spotify.apollo.request.Handlers.requestHandler;
import static com.spotify.apollo.request.Handlers.requestRunnableFactory;
import static com.spotify.apollo.request.Handlers.withGathering;

/**
 * A module setting up implementations of Apollo API Framework components such as {@link
 * Environment}, {@link Environment.RoutingEngine} and a managed {@link Client}.
 *
 * This module can be used to construct a {@link RequestHandler} from an {@link AppInit} that can
 * be used with server modules.
 */
public class ApolloEnvironmentModule extends AbstractApolloModule {

  private static final Logger LOG = LoggerFactory.getLogger(ApolloEnvironmentModule.class);

  public static ApolloEnvironmentModule create() {
    return new ApolloEnvironmentModule();
  }

  /**
   * Gets the {@link ApolloEnvironment} that can be used to initialize applications.
   *
   * @param instance The Service instance that is being set up
   * @return an ApolloEnvironment ready to be used for initialization.
   */
  public static ApolloEnvironment environment(Service.Instance instance) {
    return instance.resolve(ApolloEnvironment.class);
  }

  @Override
  protected void configure() {
    // just declare the multibinders so that we can inject the empty set
    Multibinder.newSetBinder(binder(), ClientDecorator.class);
    Multibinder.newSetBinder(binder(), RequestRunnableFactoryDecorator.class);
    Multibinder.newSetBinder(binder(), EndpointRunnableFactoryDecorator.class);

    bind(ApolloConfig.class).in(Singleton.class); // used by most sub-modules
    bind(ApolloEnvironment.class).to(ApolloEnvironmentImpl.class).in(Singleton.class);

    install(new MetaModule());
    install(new EnvironmentModule());
  }

  @Override
  public String getId() {
    return "apollo-env";
  }

  private static class ApolloEnvironmentImpl implements ApolloEnvironment {

    private final Closer closer;
    private final Config configNode;
    private final ApolloConfig apolloConfig;
    private final Environment environment;
    private final IncomingRequestAwareClient incomingRequestAwareClient;

    /*
     * ApplicationRouter from RoutingContext
     */
    private final EnvironmentFactory.RoutingContext routingContext;

    /*
     * Meta application merged with ApplicationRouter
     * TODO: maybe time to put in a separate server now?
     */
    private final MetaInfoTracker metaInfoTracker;

    /*
     * RequestHandler
     */
    private final Set<RequestRunnableFactoryDecorator> rrfDecorators;
    private final Set<EndpointRunnableFactoryDecorator> erfDecorators;

    @Inject
    private ApolloEnvironmentImpl(
        Closer closer,
        Config configNode,
        Environment environment,
        EnvironmentFactory.RoutingContext routingContext,
        IncomingRequestAwareClient incomingRequestAwareClient,
        MetaInfoTracker metaInfoTracker,
        Set<RequestRunnableFactoryDecorator> rrfDecorators,
        Set<EndpointRunnableFactoryDecorator> erfDecorators,
        ApolloConfig apolloConfig) {
      this.closer = closer;
      this.configNode = configNode;
      this.environment = environment;
      this.routingContext = routingContext;
      this.incomingRequestAwareClient = incomingRequestAwareClient;
      this.metaInfoTracker = metaInfoTracker;
      this.rrfDecorators = rrfDecorators;
      this.erfDecorators = erfDecorators;
      this.apolloConfig = apolloConfig;
    }

    @Override
    public Environment environment() {
      return environment;
    }

    @Override
    public RequestHandler initialize(AppInit appInit) {
      appInit.create(environment);

      return createRequestHandler();
    }

    private RequestHandler createRequestHandler() {
      final Object[] all = Iterables.toArray(routingContext.endpointObjects(), Object.class);
      final ApplicationRouter<Endpoint> applicationRouter = Routers.newRouterFromInspecting(all);

      final MetaGatherer gatherer = metaInfoTracker.getGatherer();

      metaInfoTracker.gatherEndpoints(applicationRouter.getRuleTargets());

      final ApplicationRouter<Endpoint> metaRouter =
          Routers.newRouterFromInspecting(new MetaApplication(gatherer));

      final ApplicationRouter<Endpoint> endpointApplicationOrMetaRouter =
          apolloConfig.enableMetaApi()
          ? new ApplicationOrMetaRouter<>(applicationRouter, metaRouter)
          : applicationRouter;

      final RequestRunnableFactory baseRequestRunnableFactory =
          requestRunnableFactory(endpointApplicationOrMetaRouter);
      final EndpointRunnableFactory baseEndpointRunnableFactory =
          endpointRunnableFactory();

      final RequestRunnableFactory decoratedRequestRunnableFactory =
          foldDecorators(baseRequestRunnableFactory, rrfDecorators);
      final EndpointRunnableFactory decoratedEndpointRunnableFactory =
          foldDecorators(baseEndpointRunnableFactory, erfDecorators);

      final RequestHandler requestHandler = requestHandler(
          decoratedRequestRunnableFactory,
          withGathering(
              decoratedEndpointRunnableFactory,
              metaInfoTracker.incomingCallsGatherer()
          ),
          incomingRequestAwareClient);

      closer.register(() -> LOG.info("Shutting down Apollo instance"));

      return requestHandler;
    }
  }

  static <R> R foldDecorators(R init, Iterable<? extends Function<R, R>> decorators) {
    R fold = init;
    for (Function<R, R> decorator : decorators) {
      fold = decorator.apply(fold);
    }
    return fold;
  }
}
