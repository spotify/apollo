/*
 * -\-\-
 * Spotify Apollo API Environment
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
package com.spotify.apollo.environment;


import java.util.Optional;

/**
 * Helper functions for reading keys as {@link Optional} values.
 */
public final class ConfigUtil {
  private ConfigUtil() {}

//  public static Optional<String> optionalString(Config config, String path) {
//    return config.hasPath(path) ? Optional.of(config.getString(path)) : Optional.empty();
//  }
//
//  public static Optional<Boolean> optionalBoolean(Config config, String path) {
//    return config.hasPath(path) ? Optional.of(config.getBoolean(path)) : Optional.empty();
//  }
//
//  public static Optional<Integer> optionalInt(Config config, String path) {
//    return config.hasPath(path) ? Optional.of(config.getInt(path)) : Optional.empty();
//  }
//
//  public static Optional<Double> optionalDouble(Config config, String path) {
//    return config.hasPath(path) ? Optional.of(config.getDouble(path)) : Optional.empty();
//  }
//
//  public static Optional<Config> optionalConfig(Config config, String path) {
//    return config.hasPath(path) ? Optional.of(config.getConfig(path)) : Optional.empty();
//  }

  public static <T> Optional<T> either(Optional<T> original, Optional<T> alternative) {
    return original.isPresent() ? original : alternative;
  }
}
