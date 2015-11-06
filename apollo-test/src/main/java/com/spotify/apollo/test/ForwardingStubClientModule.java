package com.spotify.apollo.test;

import com.google.inject.multibindings.Multibinder;

import com.spotify.apollo.environment.ClientDecorator;
import com.spotify.apollo.environment.IncomingRequestAwareClient;
import com.spotify.apollo.module.AbstractApolloModule;
import com.spotify.apollo.module.ApolloModule;

class ForwardingStubClientModule extends AbstractApolloModule
    implements ClientDecorator {

  private final boolean forward;
  private final IncomingRequestAwareClient stubClient;

  private ForwardingStubClientModule(boolean forward, IncomingRequestAwareClient stubClient) {
    this.forward = forward;
    this.stubClient = stubClient;
  }

  public static ApolloModule create(boolean forward, IncomingRequestAwareClient stubClient) {
    return new ForwardingStubClientModule(forward, stubClient);
  }

  @Override
  protected void configure() {
    Multibinder.newSetBinder(binder(), ClientDecorator.class)
        .addBinding().toInstance(this);
  }

  @Override
  public String getId() {
    return "forwarding-stub-client";
  }

  @Override
  public IncomingRequestAwareClient apply(IncomingRequestAwareClient client) {
    return forward ? new FallbackClient(stubClient, client) : stubClient;
  }
}
