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

import com.google.inject.multibindings.Multibinder;
import com.spotify.apollo.environment.ClientDecorator;
import com.spotify.apollo.environment.IncomingRequestAwareClient;
import com.spotify.apollo.module.AbstractApolloModule;
import com.spotify.apollo.module.ApolloModule;
import com.squareup.okhttp.OkHttpClient;

/**
 * Module extends {@link IncomingRequestAwareClient} to be able to handle HTTP calls.
 *
 * <p>You can also use the {@link OkHttpClient} in case you don't want to use the apollo client, but
 * doing so won't provide you with metrics (which still needs to be enabled by using the {@link
 * HttpMetricModule}).
 *
 * @see OkHttpClientProvider
 * @see HttpMetricModule
 * @see IncomingRequestAwareClient
 * @see com.spotify.apollo.Client
 * @see com.spotify.apollo.Environment#client()
 */
public class HttpClientModule extends AbstractApolloModule {

  private HttpClientModule() {}

  public static ApolloModule create() {
    return new HttpClientModule();
  }

  @Override
  protected void configure() {
    Multibinder.newSetBinder(binder(), ClientDecorator.class)
        .addBinding()
        .to(HttpClientDecorator.class);

    bind(HttpClient.class);
    bind(OkHttpClient.class).toProvider(OkHttpClientProvider.class);
  }

  @Override
  public String getId() {
    return "http.client";
  }
}
