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

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigValue;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class DurationThresholdConfig {

  private final Optional<Integer> allEndpoints;
  private final Map<String, Optional<Integer>> goals;

  private DurationThresholdConfig(final Optional<Integer> allEndpoints,
                                  final Map<String, Optional<Integer>> goals) {
    this.allEndpoints = allEndpoints;
    this.goals = goals;
  }

  public Optional<Integer> getDurationThresholdForEndpoint(final String endpointMethod) {
    return goals.getOrDefault(endpointMethod, allEndpoints);
  }

  private static Set<Map.Entry<String, Optional<Integer>>> flattenNestedConfig(
      Map.Entry<String, ConfigValue> entry) {
    return ((Map<String, Integer>) entry.getValue().unwrapped())
        .entrySet()
        .stream()
        .collect(
            Collectors.toMap(
                methodEntry -> String.format("%s:%s", methodEntry.getKey(), entry.getKey()),
                methodEntry -> Optional.of(methodEntry.getValue())
            )
        ).entrySet();
  }

  private static Optional<Integer> parseAllEndpoints(final Config config) {
    final Optional<Integer> allEndpoints;
    final Config durationGoalConfig = config.getObject("endpoint-duration-goal").toConfig();
    if (durationGoalConfig.hasPath("all-endpoints")) {
      allEndpoints = Optional.of(durationGoalConfig.getInt("all-endpoints"));
    } else {
      allEndpoints = Optional.empty();
    }
    return allEndpoints;
  }

  public static DurationThresholdConfig parseConfig(final Config config) {
    if (config.hasPath("endpoint-duration-goal")) {
      final Map<String, Optional<Integer>> goals =
          config.getObject("endpoint-duration-goal")
              .withoutKey("all-endpoints")
              .entrySet()
              .stream()
              .map(entry -> flattenNestedConfig(entry))
              .flatMap(Collection::stream)
              .collect(
                  Collectors.toMap(
                      Map.Entry::getKey,
                      Map.Entry::getValue));
      return new DurationThresholdConfig(parseAllEndpoints(config), goals);
    }
    return new DurationThresholdConfig(Optional.empty(), ImmutableMap.of());
  }
}

