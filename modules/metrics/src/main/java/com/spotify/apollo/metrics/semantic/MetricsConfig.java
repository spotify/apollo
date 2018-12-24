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
package com.spotify.apollo.metrics.semantic;

import static com.spotify.apollo.environment.ConfigUtil.optionalInt;
import static com.spotify.apollo.metrics.semantic.What.DROPPED_REQUEST_RATE;
import static com.spotify.apollo.metrics.semantic.What.ENDPOINT_REQUEST_DURATION;
import static com.spotify.apollo.metrics.semantic.What.ENDPOINT_REQUEST_DURATION_THRESHOLD_RATE;
import static com.spotify.apollo.metrics.semantic.What.ENDPOINT_REQUEST_RATE;
import static com.spotify.apollo.metrics.semantic.What.ERROR_RATIO;

import com.google.inject.Inject;
import com.typesafe.config.Config;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class MetricsConfig {

  // visible for testing
  @SuppressWarnings("WeakerAccess")
  static final Set<What> DEFAULT_ENABLED_METRICS =
      EnumSet.of(
          ENDPOINT_REQUEST_RATE,
          ENDPOINT_REQUEST_DURATION,
          ENDPOINT_REQUEST_DURATION_THRESHOLD_RATE,
          DROPPED_REQUEST_RATE,
          ERROR_RATIO);

  static final int DEFAULT_TTL_SECONDS = (int) TimeUnit.MINUTES.toSeconds(5);

  private final Set<What> enabledMetrics;
  private final Set<Integer> precreateCodes;
  private final DurationThresholdConfig durationThresholdConfig;
  private Config config;

  @Inject
  MetricsConfig(Config config) {
    this.config = config;

    if (config.hasPath("metrics.server")) {
      enabledMetrics = parseConfig(config.getStringList("metrics.server"));
    } else {
      enabledMetrics = DEFAULT_ENABLED_METRICS;
    }

    if (config.hasPath("metrics.precreate-codes")) {
      precreateCodes = new HashSet<>(config.getIntList("metrics.precreate-codes"));
    } else {
      precreateCodes = Collections.emptySet();
    }

    durationThresholdConfig = DurationThresholdConfig.parseConfig(config);

  }

  private Set<What> parseConfig(List<String> metrics) {
    EnumSet<What> result = EnumSet.noneOf(What.class);

    for (String metricName : metrics) {
      result.add(What.valueOf(metricName));
    }

    return result;
  }

  public DurationThresholdConfig durationThresholdConfig() { return durationThresholdConfig; }

  public Set<What> serverMetrics() {
    return enabledMetrics;
  }

  public Set<Integer> precreateCodes() {
    return precreateCodes;
  }

  public int reservoirTtl() {
    return optionalInt(config, "metrics.reservoir-ttl").orElse(DEFAULT_TTL_SECONDS);
  }
}
