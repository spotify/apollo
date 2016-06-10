/*
 * -\-\-
 * Spotify Apollo okhttp Client Module
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
package com.spotify.apollo.http.client;

import java.time.Duration;
import java.util.Optional;

/**
 * TODO: document!
 */
public class OkHttpClientConfiguration {

  private final Optional<Integer> connectTimeoutMillis = Optional.empty();
  private final Optional<Integer> readTimeoutMillis = Optional.empty();
  private final Optional<Integer> writeTimeoutMillis = Optional.empty();
  private final Optional<Integer> maxAsyncRequests = Optional.empty();
  private final Optional<Integer> maxAsyncRequestsPerHost = Optional.empty();

  // defaults that come from com.squareup.okhttp.ConnectionPool
  private final int maxIdleConnections = 5;
  private final long connectionKeepAliveDurationMillis = Duration.ofMinutes(5).toMillis();


  public Optional<Integer> connectTimeoutMillis() {
    return connectTimeoutMillis;
  }

  public Optional<Integer> readTimeoutMillis() {
    return readTimeoutMillis;
  }

  public Optional<Integer> writeTimeoutMillis() {
    return writeTimeoutMillis;
  }

  public int maxIdleConnections() {
    return maxIdleConnections;
  }

  public long connectionKeepAliveDurationMillis() {
    return connectionKeepAliveDurationMillis;
  }

  public Optional<Integer> maxAsyncRequests() {
    return maxAsyncRequests;
  }

  public Optional<Integer> maxAsyncRequestsPerHost() {
    return maxAsyncRequestsPerHost;
  }
}
