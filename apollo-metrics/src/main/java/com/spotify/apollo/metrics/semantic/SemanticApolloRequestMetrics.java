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

import com.google.common.base.Preconditions;

import com.spotify.apollo.metrics.ApolloRequestMetrics;
import com.spotify.apollo.metrics.ApolloTimerContext;
import com.spotify.metrics.core.MetricId;
import com.spotify.metrics.core.SemanticMetricRegistry;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

class SemanticApolloRequestMetrics implements ApolloRequestMetrics {

  private static final String COMPONENT = "service-request";
  private final SemanticMetricRegistry metricRegistry;

  private final MetricId fanoutId;
  private final MetricId countRequestId;
  private final MetricId timeRequestId;

  private final ConcurrentMap<Integer, MetricId> statusCodeIds;

  public SemanticApolloRequestMetrics(
      SemanticMetricRegistry metricRegistry,
      MetricId id) {
    this.metricRegistry = metricRegistry;
    // Already tagged with 'service' and 'endpoint'. 'application' gets added by the ffwd reporter
    Preconditions.checkArgument(id.getTags().containsKey("service"),
                                "metricId must be tagged with 'service'");
    Preconditions.checkArgument(id.getTags().containsKey("endpoint"),
                                "metricId must be tagged with 'endpoint'");

    MetricId metricId = id.tagged("component", COMPONENT);

    fanoutId = metricId.tagged(
        "what", "request-fanout-factor",
        "unit", "request/request");

    countRequestId = metricId.tagged(
        "what", "endpoint-request-rate",
        "unit", "request");

    timeRequestId = metricId.tagged(
        "what", "endpoint-request-duration");

    statusCodeIds = new ConcurrentHashMap<>();
  }

  @Override
  public void fanout(int requests) {
    metricRegistry.histogram(fanoutId).update(requests);
  }

  @Override
  public void countRequest(int statusCode) {
    MetricId statusCodeId = statusCodeIds.computeIfAbsent(
        statusCode, code -> countRequestId.tagged("status-code", "" + code));
    metricRegistry.meter(statusCodeId).mark();
  }

  @Override
  public ApolloTimerContext timeRequest() {
    return new SemanticApolloTimerContext(metricRegistry.timer(timeRequestId).time());
  }
}
