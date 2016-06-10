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
