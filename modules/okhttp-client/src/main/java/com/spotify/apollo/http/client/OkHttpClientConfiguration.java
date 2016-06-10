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
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class OkHttpClientConfiguration {

  private final Optional<Duration> connectTimeout;
  private final Optional<Duration> readTimeout;
  private final Optional<Duration> writeTimeout;
  private final Optional<Integer> maxAsyncRequests;
  private final Optional<Integer> maxAsyncRequestsPerHost;

  // defaults that come from com.squareup.okhttp.ConnectionPool
  private final int maxIdleConnections;
  private final long connectionKeepAliveDurationMillis;

  private OkHttpClientConfiguration(Optional<Duration> connectTimeout,
                                    Optional<Duration> readTimeout,
                                    Optional<Duration> writeTimeout,
                                    Optional<Integer> maxAsyncRequests,
                                    Optional<Integer> maxAsyncRequestsPerHost,
                                    int maxIdleConnections,
                                    long connectionKeepAliveDurationMillis) {
    this.connectionKeepAliveDurationMillis = connectionKeepAliveDurationMillis;
    this.maxIdleConnections = maxIdleConnections;
    this.connectTimeout = connectTimeout;
    this.readTimeout = readTimeout;
    this.writeTimeout = writeTimeout;
    this.maxAsyncRequests = maxAsyncRequests;
    this.maxAsyncRequestsPerHost = maxAsyncRequestsPerHost;
  }


  public Optional<Duration> connectTimeout() {
    return connectTimeout;
  }

  public Optional<Duration> readTimeout() {
    return readTimeout;
  }

  public Optional<Duration> writeTimeout() {
    return writeTimeout;
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

  public OkHttpClientConfiguration withConnectTimeout(Duration timeout) {
    return new OkHttpClientConfiguration(Optional.of(timeout),
                                         readTimeout,
                                         writeTimeout,
                                         maxAsyncRequests,
                                         maxAsyncRequestsPerHost,
                                         maxIdleConnections,
                                         connectionKeepAliveDurationMillis);
  }

  public static OkHttpClientConfiguration create() {
    return new OkHttpClientConfiguration(Optional.empty(), Optional.empty(), Optional.empty(),
                                         Optional.empty(), Optional.empty(), 5,
                                         Duration.ofMinutes(5).toMillis()
    );
  }

  public OkHttpClientConfiguration withReadTimeout(Duration timeout) {
    return new OkHttpClientConfiguration(connectTimeout,
                                         Optional.of(timeout),
                                         writeTimeout,
                                         maxAsyncRequests,
                                         maxAsyncRequestsPerHost,
                                         maxIdleConnections,
                                         connectionKeepAliveDurationMillis);
  }

  public OkHttpClientConfiguration withWriteTimeout(Duration timeout) {
    return new OkHttpClientConfiguration(connectTimeout,
                                         readTimeout,
                                         Optional.of(timeout),
                                         maxAsyncRequests,
                                         maxAsyncRequestsPerHost,
                                         maxIdleConnections,
                                         connectionKeepAliveDurationMillis);
  }

  public OkHttpClientConfiguration withMaxAsyncRequests(int maxRequests) {
    return new OkHttpClientConfiguration(connectTimeout,
                                         readTimeout,
                                         writeTimeout,
                                         Optional.of(maxRequests),
                                         maxAsyncRequestsPerHost,
                                         maxIdleConnections,
                                         connectionKeepAliveDurationMillis);
  }

  public OkHttpClientConfiguration withMaxAsyncRequestsPerHost(int max) {
    return new OkHttpClientConfiguration(connectTimeout,
                                         readTimeout,
                                         writeTimeout,
                                         maxAsyncRequests,
                                         Optional.of(max),
                                         maxIdleConnections,
                                         connectionKeepAliveDurationMillis);
  }
}
