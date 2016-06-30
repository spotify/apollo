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

import com.spotify.apollo.metrics.MetricsFactory;
import com.spotify.apollo.metrics.ServiceMetrics;
import com.spotify.metrics.core.MetricId;
import com.spotify.metrics.core.SemanticMetricRegistry;

import java.util.Set;

public class SemanticMetricsFactory implements MetricsFactory {

  private final SemanticMetricRegistry metricRegistry;
  private final MetricId metricId;
  private final Set<Metric> enabledMetrics;

  public SemanticMetricsFactory(final SemanticMetricRegistry metricRegistry,
                                Set<Metric> enabledMetrics) {
    this.metricRegistry = metricRegistry;
    this.metricId = MetricId.build();
    this.enabledMetrics = enabledMetrics;
  }

  @Override
  public ServiceMetrics createForService(String serviceName) {
    final MetricId id = metricId.tagged("service", serviceName);
    return new SemanticServiceMetrics(metricRegistry, id, enabledMetrics);
  }
}
