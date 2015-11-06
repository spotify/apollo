package com.spotify.apollo.environment;

import com.google.inject.multibindings.Multibinder;

import com.spotify.apollo.module.AbstractApolloModule;
import com.spotify.apollo.request.RequestRunnableFactory;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

/**
 * A module used for testing. It counts and asserts requests against a certain uri.
 */
class RequestInspectingModule extends AbstractApolloModule
    implements RequestRunnableFactoryDecorator {

  private String matchUri;
  private final AtomicInteger requestCounter;

  RequestInspectingModule(String matchUri, AtomicInteger requestCounter) {
    this.matchUri = matchUri;
    this.requestCounter = requestCounter;
  }

  @Override
  protected void configure() {
    Multibinder.newSetBinder(binder(), RequestRunnableFactoryDecorator.class)
        .addBinding().toInstance(this);
  }

  @Override
  public String getId() {
    return "request-inspector";
  }

  @Override
  public RequestRunnableFactory apply(RequestRunnableFactory requestRunnableFactory) {
    return request -> {
      assertEquals(matchUri, request.request().uri());
      requestCounter.incrementAndGet();
      return requestRunnableFactory.create(request);
    };
  }
}
