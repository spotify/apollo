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

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.RatioGauge;
import com.spotify.apollo.StatusType;
import com.spotify.apollo.metrics.RequestMetrics;
import com.spotify.apollo.metrics.TimerContext;
import com.spotify.metrics.core.MetricId;
import com.spotify.metrics.core.SemanticMetricRegistry;

import java.util.function.Supplier;

import static com.spotify.apollo.StatusType.Family.INFORMATIONAL;
import static com.spotify.apollo.StatusType.Family.SUCCESSFUL;
import static java.util.Objects.requireNonNull;

class SemanticRequestMetrics implements RequestMetrics {

  private static final String COMPONENT = "service-request";
  private final SemanticMetricRegistry metricRegistry;

  private final MetricId countRequestId;
  private final MetricId timeRequestId;
  private final Meter sentReplies;
  private final Meter sentErrors;
  private final Histogram fanoutHistogram;

  SemanticRequestMetrics(
      SemanticMetricRegistry metricRegistry,
      MetricId id,
      Meter sentReplies,
      Meter sentErrors) {
    this.metricRegistry = metricRegistry;
    // Already tagged with 'service' and 'endpoint'. 'application' gets added by the ffwd reporter
    Preconditions.checkArgument(id.getTags().containsKey("service"),
                                "metricId must be tagged with 'service'");
    Preconditions.checkArgument(id.getTags().containsKey("endpoint"),
                                "metricId must be tagged with 'endpoint'");

    MetricId metricId = id.tagged("component", COMPONENT);

    fanoutHistogram = metricRegistry.histogram(
        metricId.tagged(
            "what", "request-fanout-factor",
            "unit", "request/request"));

    countRequestId = metricId.tagged(
        "what", "endpoint-request-rate",
        "unit", "request");

    timeRequestId = metricId.tagged(
        "what", "endpoint-request-duration");

    this.sentReplies = requireNonNull(sentReplies);
    this.sentErrors = requireNonNull(sentErrors);

    registerRatioGauge(metricId, "1m", () -> RatioGauge.Ratio.of(this.sentErrors.getOneMinuteRate(),
                                                                 this.sentReplies.getOneMinuteRate()));
    registerRatioGauge(metricId, "5m", () -> RatioGauge.Ratio.of(this.sentErrors.getFiveMinuteRate(),
                                                                 this.sentReplies.getFiveMinuteRate()));
    registerRatioGauge(metricId, "15m", () -> RatioGauge.Ratio.of(this.sentErrors.getFifteenMinuteRate(),
                                                                  this.sentReplies.getFifteenMinuteRate()));
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
    fanoutHistogram.update(requests);
  }

  @Override
  public void responseStatus(StatusType status) {
    metricRegistry.meter(countRequestId.tagged("status-code", String.valueOf(status.code()))).mark();
    sentReplies.mark();

    StatusType.Family family = status.family();
    if (family != INFORMATIONAL && family != SUCCESSFUL) {
      sentErrors.mark();
    }
  }

  @Override
  public TimerContext timeRequest() {
    return new SemanticTimerContext(metricRegistry.timer(timeRequestId).time());
  }
}
