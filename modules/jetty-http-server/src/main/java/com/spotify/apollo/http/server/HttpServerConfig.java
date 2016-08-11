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

import com.typesafe.config.Config;

import javax.inject.Inject;

import static com.spotify.apollo.environment.ConfigUtil.optionalInt;
import static com.spotify.apollo.environment.ConfigUtil.optionalString;

class HttpServerConfig {

  private static final String CONFIG_BASE_NAME = "http.server";
  private static final String DEFAULT_HTTP_ADDRESS = "0.0.0.0";
  private static final int DEFAULT_TTL_MILLIS = 30000;

  private final Config config;

  @Inject
  HttpServerConfig(Config config) {
    this.config = config;
  }

  public String address() {
    return optionalString(config, CONFIG_BASE_NAME + ".address").orElse(DEFAULT_HTTP_ADDRESS);
  }

  public Integer port() {
    return optionalInt(config, CONFIG_BASE_NAME + ".port").orElse(null);
  }

  public long ttlMillis() {
    return optionalInt(config, CONFIG_BASE_NAME + ".ttlMillis").orElse(DEFAULT_TTL_MILLIS);
  }
}
