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

import com.google.inject.Inject;
import com.google.inject.Provider;

import com.squareup.okhttp.ConnectionPool;
import com.squareup.okhttp.OkHttpClient;

import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;


class OkHttpClientProvider implements Provider<OkHttpClient> {

  private final OkHttpClientConfiguration config;

  @Inject
  OkHttpClientProvider(OkHttpClientConfiguration config) {
    this.config = requireNonNull(config);
  }

  @Override
  public OkHttpClient get() {
    final OkHttpClient client = new OkHttpClient();

    //timeouts settings
    config.connectTimeout().ifPresent(
        timeout -> client.setConnectTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS));

    config.readTimeout().ifPresent(
        timeout -> client.setReadTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS));

    config.writeTimeout().ifPresent(
        timeout -> client.setWriteTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS));

    // connection pool settings
    client.setConnectionPool(new ConnectionPool(
        config.maxIdleConnections(),
        config.connectionKeepAliveDurationMillis()
    ));

    // async dispatcher settings
    config.maxAsyncRequests().ifPresent(max -> client.getDispatcher().setMaxRequests(max));

    config.maxAsyncRequestsPerHost().ifPresent(
        max -> client.getDispatcher().setMaxRequestsPerHost(max));

    return client;
  }
}
