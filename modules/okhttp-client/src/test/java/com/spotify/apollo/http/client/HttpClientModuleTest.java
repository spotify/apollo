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
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import org.junit.Test;

import java.util.Set;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class HttpClientModuleTest {

  Service service() {
    return Services.usingName("ping")
        .withModule(HttpClientModule.create())
        .build();
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
    final Config config = ConfigFactory.parseString("http.client.connectTimeout: 7982");

    final Service service = Services.usingName("test")
        .withModule(HttpClientModule.create())
        .build();

    try (Service.Instance i = service.start(new String[] {}, config)) {

      final OkHttpClient underlying = i.resolve(OkHttpClient.class);
      assertThat(underlying.getConnectTimeout(), is(7982));
    }

  }

}
