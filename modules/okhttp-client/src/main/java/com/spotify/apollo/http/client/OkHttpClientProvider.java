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

import com.google.common.io.Closer;
import com.google.inject.Inject;
import com.google.inject.Provider;

import com.spotify.apollo.concurrent.ExecutorServiceCloser;
import com.typesafe.config.Config;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;

import static com.spotify.apollo.environment.ConfigUtil.optionalBoolean;
import static com.spotify.apollo.environment.ConfigUtil.optionalInt;

class OkHttpClientProvider implements Provider<OkHttpClient> {

  private final OkHttpClientConfig config;
  private final Closer closer;

  @Inject
  OkHttpClientProvider(Config config, Closer closer) {
    this.config = new OkHttpClientConfig(config);
    this.closer = closer;
  }

  @Override
  public OkHttpClient get() {
    final OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();

    //timeouts settings
    config.connectTimeoutMillis().ifPresent(
        millis -> clientBuilder.connectTimeout(millis, TimeUnit.MILLISECONDS));

    config.readTimeoutMillis().ifPresent(
        millis -> clientBuilder.readTimeout(millis, TimeUnit.MILLISECONDS));

    config.writeTimeoutMillis().ifPresent(
        millis -> clientBuilder.writeTimeout(millis, TimeUnit.MILLISECONDS));

    // connection pool settings
    clientBuilder.connectionPool(new ConnectionPool(
        // defaults that come from com.squareup.okhttp.ConnectionPool
        config.maxIdleConnections().orElse(5),
        config.connectionKeepAliveDurationMillis().orElse(5 * 60 * 1000),
        TimeUnit.MILLISECONDS
    ));

    config.followRedirects().ifPresent(clientBuilder::followRedirects);

    final OkHttpClient client = clientBuilder.build();

    // async dispatcher settings
    config.maxAsyncRequests().ifPresent(max -> client.dispatcher().setMaxRequests(max));

    config.maxAsyncRequestsPerHost().ifPresent(
        max -> client.dispatcher().setMaxRequestsPerHost(max));

    closer.register(ExecutorServiceCloser.of(client.dispatcher().executorService()));

    return client;
  }

  private static class OkHttpClientConfig {

    private final Config config;

    OkHttpClientConfig(final Config config) {
      this.config = config;
    }

    Optional<Integer> connectTimeoutMillis() {
      return optionalInt(config, "http.client.connectTimeout");
    }

    Optional<Integer> readTimeoutMillis() {
      return optionalInt(config, "http.client.readTimeout");
    }

    Optional<Integer> writeTimeoutMillis() {
      return optionalInt(config, "http.client.writeTimeout");
    }

    Optional<Integer> maxIdleConnections() {
      return optionalInt(config, "http.client.maxIdleConnections");
    }

    Optional<Integer> connectionKeepAliveDurationMillis() {
      return optionalInt(config, "http.client.keepAliveDuration");
    }

    Optional<Integer> maxAsyncRequests() {
      return optionalInt(config, "http.client.async.maxRequests");
    }

    Optional<Integer> maxAsyncRequestsPerHost() {
      return optionalInt(config, "http.client.async.maxRequestsPerHost");
    }

    Optional<Boolean> followRedirects() {
      return optionalBoolean(config, "http.client.followRedirects");
    }
  }
}
