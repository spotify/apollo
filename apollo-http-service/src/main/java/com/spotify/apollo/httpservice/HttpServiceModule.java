/*
 * Copyright (c) 2013-2014 Spotify AB
 */

package com.spotify.apollo.httpservice;

import com.spotify.apollo.AppInit;
import com.spotify.apollo.core.Service;
import com.spotify.apollo.environment.ApolloEnvironment;
import com.spotify.apollo.environment.ApolloEnvironmentModule;
import com.spotify.apollo.module.AbstractApolloModule;
import com.spotify.apollo.request.RequestHandler;

/**
 * Used to gather dependencies from other modules into {@link HttpService}
 */
class HttpServiceModule extends AbstractApolloModule {

  private final AppInit appInit;

  private HttpServiceModule(AppInit appInit) {
    this.appInit = appInit;
  }

  public static HttpServiceModule create(AppInit appInit) {
    return new HttpServiceModule(appInit);
  }

  @Override
  protected void configure() {
    bindAppInit();

    install(ApolloEnvironmentModule.create());
  }

  private void bindAppInit() {
    bind(Initializer.class).toInstance(env -> env.initialize(appInit));
  }

  static RequestHandler requestHandler(Service.Instance instance) {
    final ApolloEnvironment environment = ApolloEnvironmentModule.environment(instance);
    final Initializer initializer = instance.resolve(Initializer.class);
    return initializer.init(environment);
  }

  @Override
  public String getId() {
    return "apollo-http-service";
  }

  private interface Initializer {
    RequestHandler init(ApolloEnvironment apolloEnvironment);
  }
}
