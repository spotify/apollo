/*
 * -\-\-
 * Spotify Apollo okhttp Client Module
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
package com.spotify.apollo.http.client;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

import com.google.inject.Binder;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.spotify.apollo.core.Service;
import com.spotify.apollo.core.Services;
import com.spotify.apollo.environment.ClientDecorator;
import com.spotify.apollo.environment.IncomingRequestAwareClient;
import com.spotify.apollo.http.client.HttpMetricModule.HttpMetricClientDecorator;
import com.spotify.apollo.module.ApolloModule;
import com.spotify.metrics.core.SemanticMetricRegistry;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;

class HttpMetricModuleTest {

  private SemanticMetricRegistry mockedMetricRegistry;
  private IncomingRequestAwareClient mockedClient;
  private Service service;

  @Before
  public void setup() {
    service =
        Services.usingName("httpMetricModuleTest")
            .withModule(
                new ApolloModule() {
                  @Override
                  public String getId() {
                    return "testing-";
                  }

                  @Override
                  public double getPriority() {
                    return 0;
                  }

                  @Override
                  public Set<? extends Key<?>> getLifecycleManaged() {
                    return Collections.emptySet();
                  }

                  @Override
                  public void configure(Binder binder) {
                    mockedMetricRegistry = mock(SemanticMetricRegistry.class);
                    binder.bind(SemanticMetricRegistry.class).toInstance(mockedMetricRegistry);
                    mockedClient =
                        mock(IncomingRequestAwareClient.class, Answers.CALLS_REAL_METHODS);
                    binder.bind(IncomingRequestAwareClient.class).toInstance(mockedClient);
                  }
                })
            .withModule(HttpMetricModule.create())
            .build();
  }

  @Test
  public void testShouldHaveDecoratorConfigured() throws IOException {
    // given
    try (Service.Instance i = service.start()) {
      final Injector injector = i.resolve(Injector.class);

      // when
      final Set<ClientDecorator> clientDecorators =
          injector.getInstance(new Key<Set<ClientDecorator>>() {});

      // then
      assertThat(clientDecorators, hasItem(instanceOf(HttpMetricClientDecorator.class)));
    }
  }
}
