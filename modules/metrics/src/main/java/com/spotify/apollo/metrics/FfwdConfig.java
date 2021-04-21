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

import static com.spotify.apollo.environment.ConfigUtil.optionalBoolean;
import static com.spotify.apollo.environment.ConfigUtil.optionalInt;
import static com.spotify.apollo.environment.ConfigUtil.optionalString;

import com.spotify.metrics.core.MetricId;
import com.spotify.metrics.core.SemanticMetricRegistry;
import com.spotify.metrics.ffwd.FastForwardReporter;
import com.spotify.metrics.tags.EnvironmentTagExtractor;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

interface FfwdConfig {
  int DEFAULT_INTERVAL = 30;

  /**
   * Build a ffwd reporter, returning a lifecycle to be managed by Apollo.
   */
  Callable<FastForwardLifecycle> setup(
      final SemanticMetricRegistry metricRegistry, final MetricId metricId,
      final String searchDomain
  );

  static FfwdConfig fromConfig(final Config root) {
    final Config config = root.hasPath("ffwd") ? root.getConfig("ffwd") : ConfigFactory.empty();

    final String type = optionalString(config, "type").orElse("agent");

    final int interval = optionalInt(config, "interval").orElse(DEFAULT_INTERVAL);
    final boolean flush = optionalBoolean(config, "flush").orElse(Boolean.TRUE);

    switch (type) {
      case "agent":
        final Optional<String> host = optionalString(config, "host");
        final Optional<Integer> port = optionalInt(config, "port");
        return new Agent(interval, flush, host, port);
      case "http":
        throw new RuntimeException("ffwd type 'http' no longer supported");
      default:
        throw new RuntimeException("Unrecognized ffwd type: " + type);
    }
  }

  class Agent implements FfwdConfig {
    private final int interval;
    private final boolean flush;
    private final Optional<String> host;
    private final Optional<Integer> port;

    Agent(final int interval, final boolean flush, final Optional<String> host,
     final Optional<Integer> port) {
      this.interval = interval;
      this.flush = flush;
      this.host = host;
      this.port = port;
    }

    int getInterval() {
      return interval;
    }

    boolean getFlush() {
      return flush;
    }

    Optional<String> getHost() {
      return host;
    }

    Optional<Integer> getPort() {
      return port;
    }

    @Override
    public Callable<FastForwardLifecycle> setup(
        final SemanticMetricRegistry metricRegistry, final MetricId metricId,
        final String searchDomain
    ) {
      final FastForwardReporter.Builder builder = FastForwardReporter
          .forRegistry(metricRegistry)
          .schedule(TimeUnit.SECONDS, interval)
          .tagExtractor(new EnvironmentTagExtractor())
          .prefix(metricId);

      host.ifPresent(builder::host);
      port.ifPresent(builder::port);

      return () -> {
        final FastForwardReporter reporter = builder.build();
        reporter.start();
        return flush ? reporter::stopWithFlush : reporter::stop;
      };
    }
  }
}
