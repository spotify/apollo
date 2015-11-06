package com.spotify.apollo.http.client;

import com.google.inject.multibindings.Multibinder;

import com.spotify.apollo.environment.ClientDecorator;
import com.spotify.apollo.module.AbstractApolloModule;
import com.spotify.apollo.module.ApolloModule;

public class HttpClientModule extends AbstractApolloModule {

  HttpClientModule() {
  }

  public static ApolloModule create() {
    return new HttpClientModule();
  }

  @Override
  protected void configure() {
    Multibinder.newSetBinder(binder(), ClientDecorator.class)
        .addBinding().toProvider(HttpClientDecoratorProvider.class);
  }

  @Override
  public String getId() {
    return "http.client";
  }
}
