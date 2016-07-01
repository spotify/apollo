/*
 * -\-\-
 * Spotify Apollo Metrics Module
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
package com.spotify.apollo.metrics;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.util.Optional;

import javax.inject.Inject;

import static com.spotify.apollo.environment.ConfigUtil.optionalInt;
import static com.spotify.apollo.environment.ConfigUtil.optionalString;

class FfwdConfig {

  private static final int DEFAULT_INTERVAL = 30;

  private final Config configNode;

  @Inject
  FfwdConfig(Config config) {
    if (config.hasPath("ffwd")) {
      this.configNode = config.getConfig("ffwd");
    } else {
      this.configNode = ConfigFactory.empty();
    }
  }

  int getInterval() {
    return optionalInt(configNode, "interval").orElse(DEFAULT_INTERVAL);
  }

  Optional<String> host() {
    return optionalString(configNode, "host");
  }

  Optional<Integer> port() {
    return optionalInt(configNode, "port");
  }
}
