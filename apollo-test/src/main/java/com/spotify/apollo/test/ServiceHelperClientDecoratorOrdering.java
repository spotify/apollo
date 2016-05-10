/*
 * -\-\-
 * Spotify Apollo Testing Helpers
 * --
 * Copyright (C) 2013 - 2016 Spotify AB
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

import com.google.inject.TypeLiteral;

import com.spotify.apollo.environment.ClientDecorator;
import com.spotify.apollo.environment.ListClientDecoratorComparator;
import com.spotify.apollo.meta.OutgoingCallsDecorator;
import com.spotify.apollo.module.AbstractApolloModule;

import java.util.Arrays;
import java.util.Comparator;

/**
 * Ensures that client decorators are ordered correctly.
 */
public class ServiceHelperClientDecoratorOrdering extends AbstractApolloModule {

  @Override
  protected void configure() {
    bindClientDecoratorComparator();
  }

  private void bindClientDecoratorComparator() {
    // ensure that clients track outgoing calls first in the chain, and then apply the stub client
    bind(new TypeLiteral<Comparator<ClientDecorator>>() { })
        .toInstance(new ListClientDecoratorComparator(Arrays.asList(OutgoingCallsDecorator.class, ForwardingStubClientModule.class))
        );
  }

  @Override
  public String getId() {
    return "service-helper-ordering";
  }
}
