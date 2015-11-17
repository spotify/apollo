/*
 * -\-\-
 * Spotify Apollo Testing Helpers
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
