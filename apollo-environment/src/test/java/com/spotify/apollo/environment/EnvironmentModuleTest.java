/*
 * -\-\-
 * Spotify Apollo API Environment
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
package com.spotify.apollo.environment;

import com.google.common.io.Closer;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.multibindings.Multibinder;

import com.spotify.apollo.Request;
import com.spotify.apollo.core.Services;
import com.typesafe.config.Config;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import javax.inject.Named;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class EnvironmentModuleTest {
  private List<String> decoratorNames;

  @Before
  public void setUp() throws Exception {
    decoratorNames = new ArrayList<>();
  }

  @Test
  public void shouldSortClientDecoratorsIfComparatorProvided() throws Exception {
    Injector injector = Guice.createInjector(
        EnvironmentModule.create((left, right) -> left.id().compareTo(right.id())), new ModuleDeps());

    // next line will throw an exception if dependencies missing
    IncomingRequestAwareClient client = injector.getInstance(IncomingRequestAwareClient.class);

    client.send(Request.forUri("fie"), Optional.empty()).toCompletableFuture().get();

    assertThat(decoratorNames, equalTo(Arrays.asList("A", "B", "C", "D", "E")));
  }

  private class ModuleDeps extends AbstractModule {

    @Override
    protected void configure() {
      Multibinder<ClientDecorator>
          clientDecoratorMultibinder =
          Multibinder.newSetBinder(binder(), ClientDecorator.class);

      clientDecoratorMultibinder.addBinding().toInstance(new B());
      clientDecoratorMultibinder.addBinding().toInstance(new A());
      clientDecoratorMultibinder.addBinding().toInstance(new D());
      clientDecoratorMultibinder.addBinding().toInstance(new C());
      clientDecoratorMultibinder.addBinding().toInstance(new E());
    }


    @Provides
    @Named(Services.INJECT_SERVICE_NAME)
    String serviceName() {
      return "environment module test";
    }

    @Provides
    Config config() {
      return mock(Config.class);
    }

    @Provides
    ApolloConfig apolloConfig() {
      return mock(ApolloConfig.class);
    }

    @Provides
    Closer closer() {
      return mock(Closer.class);
    }
  }

  private class A extends IdentityClientDecorator { }
  private class B extends IdentityClientDecorator { }
  private class C extends IdentityClientDecorator { }
  private class D extends IdentityClientDecorator { }
  private class E extends IdentityClientDecorator { }

  private abstract class IdentityClientDecorator implements ClientDecorator {
    @Override
    public IncomingRequestAwareClient apply(IncomingRequestAwareClient incomingRequestAwareClient) {

      return ((request, incoming) -> {
        decoratorNames.add(getClass().getSimpleName());

        if (incomingRequestAwareClient instanceof NoopClient) {
          return CompletableFuture.completedFuture(null);
        }

        return incomingRequestAwareClient.send(request, incoming);
      });
    }

    @Override
    public Id id() {
      return Id.of(IdentityClientDecorator.class, getClass().getSimpleName());
    }
  }
}