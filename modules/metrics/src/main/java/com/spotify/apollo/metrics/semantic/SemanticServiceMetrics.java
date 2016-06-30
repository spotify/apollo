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

import com.codahale.metrics.Meter;
import com.spotify.apollo.metrics.RequestMetrics;
import com.spotify.apollo.metrics.ServiceMetrics;
import com.spotify.metrics.core.MetricId;
import com.spotify.metrics.core.SemanticMetricRegistry;

import java.util.Set;

class SemanticServiceMetrics implements ServiceMetrics {
  private static final String COMPONENT = "scope-factory";
  private final SemanticMetricRegistry metricRegistry;
  private final MetricId metricId;
  private final Meter sentReplies;
  private final Meter sentErrors;
  private final Set<What> enabledMetrics;

  SemanticServiceMetrics(SemanticMetricRegistry metricRegistry,
                         MetricId id,
                         Set<What> enabledMetrics) {
    this.metricRegistry = metricRegistry;
    // Already tagged with 'application' and 'service'
    this.metricId = id.tagged("component", COMPONENT);
    sentReplies = new Meter();
    sentErrors = new Meter();
    this.enabledMetrics = enabledMetrics;
  }

  @Override
  public RequestMetrics metricsForEndpointCall(String endpoint) {
    final MetricId id = metricId.tagged("endpoint", endpoint);
    return new SemanticRequestMetrics(enabledMetrics, metricRegistry, id, sentReplies, sentErrors);
  }
}
