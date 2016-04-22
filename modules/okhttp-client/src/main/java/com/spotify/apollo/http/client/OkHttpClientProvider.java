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
import com.typesafe.config.Config;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.spotify.apollo.environment.ConfigUtil.optionalInt;

class OkHttpClientProvider implements Provider<OkHttpClient> {

  private final OkHttpClientConfig config;

  @Inject
  OkHttpClientProvider(Config config) {
    this.config = new OkHttpClientConfig(config);
  }

  @Override
  public OkHttpClient get() {
    final OkHttpClient client = new OkHttpClient();

    //timeouts settings
    config.connectTimeoutMillis().ifPresent(
        millis -> client.setConnectTimeout(millis, TimeUnit.MILLISECONDS));

    config.readTimeoutMillis().ifPresent(
        millis -> client.setReadTimeout(millis, TimeUnit.MILLISECONDS));

    config.writeTimeoutMillis().ifPresent(
        millis -> client.setWriteTimeout(millis, TimeUnit.MILLISECONDS));

    // connection pool settings
    client.setConnectionPool(new ConnectionPool(
        // defaults that come from com.squareup.okhttp.ConnectionPool
        config.maxIdleConnections().orElse(5),
        config.connectionKeepAliveDurationMillis().orElse(5 * 60 * 1000)
    ));

    // async dispatcher settings
    config.maxAsyncRequests().ifPresent(max -> client.getDispatcher().setMaxRequests(max));

    config.maxAsyncRequestsPerHost().ifPresent(
        max -> client.getDispatcher().setMaxRequestsPerHost(max));

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
  }
}
