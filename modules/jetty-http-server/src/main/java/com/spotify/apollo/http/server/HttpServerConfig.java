/*
 * -\-\-
 * Spotify Apollo Jetty HTTP Server Module
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
package com.spotify.apollo.http.server;

import com.spotify.apollo.core.Services;
import com.typesafe.config.Config;

import javax.inject.Inject;
import javax.inject.Named;

import static com.spotify.apollo.environment.ConfigUtil.optionalBoolean;
import static com.spotify.apollo.environment.ConfigUtil.optionalInt;
import static com.spotify.apollo.environment.ConfigUtil.optionalString;

class HttpServerConfig {

  private static final String CONFIG_BASE_NAME = "http.server";
  private static final String DEFAULT_HTTP_ADDRESS = "0.0.0.0";
  private static final int DEFAULT_TTL_MILLIS = 30000;
  private static final int DEFAULT_WORKER_THREADS =
      Math.max(Runtime.getRuntime().availableProcessors()/4, 2);
  private static final int DEFAULT_KEEP_ALIVE_TIMEOUT = 300; // SECONDS
  private static final int DEFAULT_MAX_HTTP_CHUNK_LENGTH = 128 * 1024; // 128 kB

  private final String serviceName;
  private final Config config;

  @Inject
  HttpServerConfig(@Named(Services.INJECT_SERVICE_NAME) String serviceName, Config config) {
    this.serviceName = serviceName;
    this.config = config;
  }

  public String address() {
    return optionalString(config, CONFIG_BASE_NAME + ".address").orElse(DEFAULT_HTTP_ADDRESS);
  }

  public Integer port() {
    return optionalInt(config, CONFIG_BASE_NAME + ".port").orElse(null);
  }

  public String registrationName() {
    return optionalString(config, CONFIG_BASE_NAME + ".registrationName").orElse(serviceName);
  }

  public long ttlMillis() {
    return optionalInt(config, CONFIG_BASE_NAME + ".ttlMillis").orElse(DEFAULT_TTL_MILLIS);
  }

  public int keepAliveTimeout() {
    return optionalInt(config, CONFIG_BASE_NAME + ".keepAliveTimeout").orElse(DEFAULT_KEEP_ALIVE_TIMEOUT);
  }

  public int workerThreads() {
    return optionalInt(config, CONFIG_BASE_NAME + ".workerThreads").orElse(DEFAULT_WORKER_THREADS);
  }

  public int maxHttpChunkLength() {
    return optionalInt(config, CONFIG_BASE_NAME + ".maxHttpChunkLength").orElse(DEFAULT_MAX_HTTP_CHUNK_LENGTH);
  }

  public boolean useFirstPathSegmentAsAuthority() {
    return optionalBoolean(config, CONFIG_BASE_NAME + ".useFirstPathSegmentAsAuthority").orElse(false);
  }
}
