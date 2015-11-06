package com.spotify.apollo.http.client;

import com.spotify.apollo.environment.ClientDecorator;

import javax.inject.Provider;

class HttpClientDecoratorProvider implements Provider<ClientDecorator> {

  @Override
  public ClientDecorator get() {
    return HttpClientDecorator.create();
  }
}
