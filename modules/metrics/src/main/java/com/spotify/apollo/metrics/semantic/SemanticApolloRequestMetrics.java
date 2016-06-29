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

import com.codahale.metrics.Meter;
import com.codahale.metrics.RatioGauge;
import com.spotify.apollo.StatusType;
import com.spotify.apollo.metrics.ApolloRequestMetrics;
import com.spotify.apollo.metrics.ApolloTimerContext;
import com.spotify.metrics.core.MetricId;
import com.spotify.metrics.core.SemanticMetricRegistry;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

import static com.spotify.apollo.StatusType.Family.INFORMATIONAL;
import static com.spotify.apollo.StatusType.Family.SUCCESSFUL;

class SemanticApolloRequestMetrics implements ApolloRequestMetrics {

  private static final String COMPONENT = "service-request";
  private final SemanticMetricRegistry metricRegistry;

  private final MetricId fanoutId;
  private final MetricId countRequestId;
  private final MetricId timeRequestId;
  private final Meter sentReplies;
  private final Meter sentErrors;

  SemanticApolloRequestMetrics(
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

    sentReplies = new Meter();
    sentErrors = new Meter();

    registerRatioGauge(metricId, "1m", () -> RatioGauge.Ratio.of(sentErrors.getOneMinuteRate(),
                                                                 sentReplies.getOneMinuteRate()));
    registerRatioGauge(metricId, "5m", () -> RatioGauge.Ratio.of(sentErrors.getFiveMinuteRate(),
                                                                 sentReplies.getFiveMinuteRate()));
    registerRatioGauge(metricId, "15m", () -> RatioGauge.Ratio.of(sentErrors.getFifteenMinuteRate(),
                                                                  sentReplies.getFifteenMinuteRate()));
  }

  private void registerRatioGauge(MetricId metricId,
                                  String stat,
                                  Supplier<RatioGauge.Ratio> ratioSupplier) {
    metricRegistry.register(
        metricId.tagged("what", "error-ratio", "stat", stat),
        new RatioGauge() {
          @Override
          protected Ratio getRatio() {
            return ratioSupplier.get();
          }
        });
  }

  @Override
  public void fanout(int requests) {
    metricRegistry.histogram(fanoutId).update(requests);
  }

  @Override
  public void countRequest(int statusCode) {
    metricRegistry.meter(countRequestId.tagged("status-code", String.valueOf(statusCode))).mark();
    sentReplies.mark();

    StatusType.Family family = StatusType.Family.familyOf(statusCode);
    if (family != INFORMATIONAL && family != SUCCESSFUL) {
      sentErrors.mark();
    }
  }

  @Override
  public ApolloTimerContext timeRequest() {
    return new SemanticApolloTimerContext(metricRegistry.timer(timeRequestId).time());
  }
}