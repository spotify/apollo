/*
 * -\-\-
 * Spotify Apollo Jetty HTTP Server Module
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
package com.spotify.apollo.http.server;

import javax.annotation.Nullable;

/**
 * TODO: document!
 */
public class JettyHttpServerConfiguration {
  private static final String DEFAULT_HTTP_ADDRESS = "0.0.0.0";
  private static final long DEFAULT_TTL_MILLIS = 30000;
  private static final int DEFAULT_WORKER_THREADS =
      Math.max(Runtime.getRuntime().availableProcessors()/4, 2);
  private static final int DEFAULT_KEEP_ALIVE_TIMEOUT = 300; // SECONDS
  private static final int DEFAULT_MAX_HTTP_CHUNK_LENGTH = 128 * 1024; // 128 kB

  private final String address;
  @Nullable
  private final Integer port;
  private final String registrationName;
  private final long ttlMillis;
  private final int keepAliveTimeout;
  private final int workerThreads;
  private final int maxHttpChunkLength;
  private final boolean useFirstPathSegmentAsAuthority;

  private JettyHttpServerConfiguration(String address,
                                       @Nullable Integer port,
                                       long ttlMillis,
                                       int keepAliveTimeout,
                                       int workerThreads,
                                       int maxHttpChunkLength,
                                       boolean useFirstPathSegmentAsAuthority) {
    this.address = address;
    this.port = port;
    registrationName = null;
    this.ttlMillis = ttlMillis;
    this.keepAliveTimeout = keepAliveTimeout;
    this.workerThreads = workerThreads;
    this.maxHttpChunkLength = maxHttpChunkLength;
    this.useFirstPathSegmentAsAuthority = useFirstPathSegmentAsAuthority;
  }

  public String address() {
    return address;
  }

  @Nullable
  public Integer port() {
    return port;
  }

  public String registrationName() {
    return registrationName;
  }

  public long ttlMillis() {
    return ttlMillis;
  }

  public int keepAliveTimeout() {
    return keepAliveTimeout;
  }

  public int workerThreads() {
    return workerThreads;
  }

  public int maxHttpChunkLength() {
    return maxHttpChunkLength;
  }

  public boolean useFirstPathSegmentAsAuthority() {
    return useFirstPathSegmentAsAuthority;
  }

  public static JettyHttpServerConfiguration create() {
    return new JettyHttpServerConfiguration(DEFAULT_HTTP_ADDRESS, null, DEFAULT_TTL_MILLIS,
                                            DEFAULT_KEEP_ALIVE_TIMEOUT, DEFAULT_WORKER_THREADS,
                                            DEFAULT_MAX_HTTP_CHUNK_LENGTH, false);
  }

  public JettyHttpServerConfiguration withPort(int port) {
    return new JettyHttpServerConfiguration(address,
                                            port,
                                            ttlMillis,
                                            keepAliveTimeout,
                                            workerThreads,
                                            maxHttpChunkLength,
                                            useFirstPathSegmentAsAuthority);
  }
}
