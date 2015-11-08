package com.spotify.apollo.environment;

import com.typesafe.config.Config;

import java.util.Optional;

/**
 * Helper functions for reading keys as {@link Optional} values.
 */
public final class ConfigUtil {
  private ConfigUtil() {}

  public static Optional<String> optionalString(Config config, String path) {
    return config.hasPath(path) ? Optional.of(config.getString(path)) : Optional.empty();
  }

  public static Optional<Boolean> optionalBoolean(Config config, String path) {
    return config.hasPath(path) ? Optional.of(config.getBoolean(path)) : Optional.empty();
  }

  public static Optional<Integer> optionalInt(Config config, String path) {
    return config.hasPath(path) ? Optional.of(config.getInt(path)) : Optional.empty();
  }

  public static Optional<Double> optionalDouble(Config config, String path) {
    return config.hasPath(path) ? Optional.of(config.getDouble(path)) : Optional.empty();
  }

  public static Optional<Config> optionalConfig(Config config, String path) {
    return config.hasPath(path) ? Optional.of(config.getConfig(path)) : Optional.empty();
  }

  public static <T> Optional<T> either(Optional<T> original, Optional<T> alternative) {
    return original.isPresent() ? original : alternative;
  }
}
