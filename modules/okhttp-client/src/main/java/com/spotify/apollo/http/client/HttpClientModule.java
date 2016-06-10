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

import com.spotify.apollo.environment.ClientDecorator;
import com.spotify.apollo.environment.ClientDecorator.Id;
import com.spotify.apollo.module.AbstractApolloModule;
import com.spotify.apollo.module.ApolloModule;

import com.google.inject.multibindings.Multibinder;
import com.squareup.okhttp.OkHttpClient;

public class HttpClientModule extends AbstractApolloModule {

  public static final Id HTTP_CLIENT = Id.of(HttpClientModule.class, "HTTP client");

  private final OkHttpClientConfiguration configuration;

  private HttpClientModule(OkHttpClientConfiguration configuration) {
    this.configuration = configuration;
  }

  public static ApolloModule create(OkHttpClientConfiguration configuration) {
    return new HttpClientModule(configuration);
  }

  @Override
  protected void configure() {
    Multibinder.newSetBinder(binder(), ClientDecorator.class)
        .addBinding().to(HttpClientDecorator.class);

    bind(OkHttpClientConfiguration.class).toInstance(configuration);
    bind(HttpClient.class);
    bind(OkHttpClient.class).toProvider(OkHttpClientProvider.class);
  }

  @Override
  public String getId() {
    return "http.client";
  }
}
