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

import com.spotify.apollo.core.Service;
import com.spotify.apollo.core.Services;
import com.spotify.apollo.environment.ClientDecorator;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.squareup.okhttp.OkHttpClient;

import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.util.Set;

import static java.time.Duration.ofMillis;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class HttpClientModuleTest {

  private OkHttpClientConfiguration configuration;

  Service service() {
    return Services.usingName("ping")
        .withModule(HttpClientModule.create(configuration))
        .build();
  }

  @Before
  public void setUp() throws Exception {
    configuration = OkHttpClientConfiguration.create();
  }

  @Test
  public void testShouldInsertHttpClientDecorator() throws Exception {
    try (Service.Instance i = service().start()) {
      final Set<ClientDecorator> clientDecorators =
          i.resolve(Injector.class).getInstance(new Key<Set<ClientDecorator>>() {
          });
      assertThat(clientDecorators, hasItem(instanceOf(HttpClientDecorator.class)));
    }
  }

  /**
   * Ensure that the OkHttpClient instance provided by the module is configured as expected. This
   * duplicates tests in {@link com.spotify.apollo.http.client.OkHttpClientProviderTest} but
   * verifies that the provider is actually used.
   */
  @Test
  public void testOkHttpClientIsConfigured() throws Exception {
    final Service service = Services.usingName("test")
        .withModule(HttpClientModule.create(configuration.withConnectTimeout(ofMillis(7982))))
        .build();

    try (Service.Instance i = service.start()) {
      final OkHttpClient underlying = i.resolve(OkHttpClient.class);
      assertThat(underlying.getConnectTimeout(), is(7982));
    }
  }
}
