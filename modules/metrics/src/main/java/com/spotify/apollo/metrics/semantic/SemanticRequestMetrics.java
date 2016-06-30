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
import com.codahale.metrics.RatioGauge.Ratio;
import com.codahale.metrics.Timer;
import com.spotify.apollo.Request;
import com.spotify.apollo.Response;
import com.spotify.apollo.StatusType;
import com.spotify.apollo.metrics.RequestMetrics;
import com.spotify.metrics.core.MetricId;
import com.spotify.metrics.core.SemanticMetricRegistry;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import okio.ByteString;

import static com.spotify.apollo.StatusType.Family.INFORMATIONAL;
import static com.spotify.apollo.StatusType.Family.SUCCESSFUL;
import static com.spotify.apollo.metrics.semantic.What.DROPPED_REQUEST_RATE;
import static com.spotify.apollo.metrics.semantic.What.ENDPOINT_REQUEST_DURATION;
import static com.spotify.apollo.metrics.semantic.What.ENDPOINT_REQUEST_RATE;
import static com.spotify.apollo.metrics.semantic.What.ERROR_RATIO;
import static com.spotify.apollo.metrics.semantic.What.REQUEST_FANOUT_FACTOR;
import static com.spotify.apollo.metrics.semantic.What.REQUEST_PAYLOAD_SIZE;
import static com.spotify.apollo.metrics.semantic.What.RESPONSE_PAYLOAD_SIZE;
import static java.util.Objects.requireNonNull;

// Optional fields are fine; they enable the use of the 'ifPresent' idiom which is more readable
// than if statements
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
class SemanticRequestMetrics implements RequestMetrics {

  private final Meter sentReplies;
  private final Meter sentErrors;

  private final Optional<Consumer<Response<ByteString>>> requestRateCounter;
  private final Optional<Histogram> fanoutHistogram;
  private final Optional<Histogram> requestSizeHistogram;
  private final Optional<Histogram> responseSizeHistogram;
  private final Optional<Timer.Context> timerContext;
  private final Optional<Meter> droppedRequests;

  SemanticRequestMetrics(
      Predicate<What> enabledMetrics,
      SemanticMetricRegistry metricRegistry,
      MetricId id,
      Meter sentReplies,
      Meter sentErrors) {

    // Already tagged with 'service' and 'endpoint'. 'application' gets added by the ffwd reporter
    Preconditions.checkArgument(id.getTags().containsKey("service"),
                                "metricId must be tagged with 'service'");
    Preconditions.checkArgument(id.getTags().containsKey("endpoint"),
                                "metricId must be tagged with 'endpoint'");

    if (enabledMetrics.test(ENDPOINT_REQUEST_RATE)) {
      requestRateCounter = Optional.of(response -> metricRegistry
          .meter(id.tagged(
              "what", ENDPOINT_REQUEST_RATE.tag(),
              "unit", "request",
              "status-code", String.valueOf(response.status().code())))
          .mark());
    } else {
      requestRateCounter = Optional.empty();
    }

    if (enabledMetrics.test(REQUEST_FANOUT_FACTOR)) {
      fanoutHistogram = Optional.of(
          metricRegistry.histogram(
              id.tagged(
                  "what", REQUEST_FANOUT_FACTOR.tag(),
                  "unit", "request/request")));
    } else {
      fanoutHistogram = Optional.empty();
    }

    if (enabledMetrics.test(RESPONSE_PAYLOAD_SIZE)) {
      responseSizeHistogram = Optional.of(
          metricRegistry.histogram(
              id.tagged(
                  "what", RESPONSE_PAYLOAD_SIZE.tag(),
                  "unit", "B"
              )));
    } else {
      responseSizeHistogram = Optional.empty();
    }

    if (enabledMetrics.test(REQUEST_PAYLOAD_SIZE)) {
      requestSizeHistogram = Optional.of(
          metricRegistry.histogram(
              id.tagged(
                  "what", REQUEST_PAYLOAD_SIZE.tag(),
                  "unit", "B"
              )));
    } else {
      requestSizeHistogram = Optional.empty();
    }

    if (enabledMetrics.test(ENDPOINT_REQUEST_DURATION)) {
      timerContext = Optional.of(
          metricRegistry
              .timer(id.tagged("what", ENDPOINT_REQUEST_DURATION.tag()))
              .time());
    } else {
      timerContext = Optional.empty();
    }

    if (enabledMetrics.test(DROPPED_REQUEST_RATE)) {
      droppedRequests = Optional.of(
          metricRegistry.meter(
              id.tagged(
                  "what", DROPPED_REQUEST_RATE.tag(),
                  "unit", "request"
              )));
    } else {
      droppedRequests = Optional.empty();
    }

    this.sentReplies = requireNonNull(sentReplies);
    this.sentErrors = requireNonNull(sentErrors);

    if (enabledMetrics.test(ERROR_RATIO)) {
      registerRatioGauge(id, "1m", () -> Ratio.of(this.sentErrors.getOneMinuteRate(),
                                                  this.sentReplies.getOneMinuteRate()),
                         metricRegistry);
      registerRatioGauge(id, "5m", () -> Ratio.of(this.sentErrors.getFiveMinuteRate(),
                                                  this.sentReplies.getFiveMinuteRate()),
                         metricRegistry);
      registerRatioGauge(id, "15m", () -> Ratio.of(this.sentErrors.getFifteenMinuteRate(),
                                                   this.sentReplies.getFifteenMinuteRate()),
                         metricRegistry);
    }
  }

  private void registerRatioGauge(MetricId metricId,
                                  String stat,
                                  Supplier<Ratio> ratioSupplier,
                                  SemanticMetricRegistry metricRegistry) {
    metricRegistry.register(
        metricId.tagged("what", ERROR_RATIO.tag(), "stat", stat),
        new RatioGauge() {
          @Override
          protected Ratio getRatio() {
            return ratioSupplier.get();
          }
        });
  }

  @Override
  public void incoming(Request request) {
    requestSizeHistogram
        .ifPresent(histogram -> request.payload()
            .ifPresent(payload -> histogram.update(payload.size())));
  }

  @Override
  public void fanout(int requests) {
    fanoutHistogram.ifPresent(histogram -> histogram.update(requests));
  }

  @Override
  public void response(Response<ByteString> response) {
    requestRateCounter.ifPresent(consumer -> consumer.accept(response));
    responseSizeHistogram
        .ifPresent(histogram -> response.payload()
            .ifPresent(payload -> histogram.update(payload.size())));

    sentReplies.mark();
    timerContext.ifPresent(Timer.Context::stop);

    StatusType.Family family = response.status().family();
    if (family != INFORMATIONAL && family != SUCCESSFUL) {
      sentErrors.mark();
    }
  }

  @Override
  public void drop() {
    droppedRequests.ifPresent(Meter::mark);
    timerContext.ifPresent(Timer.Context::stop);
  }
}
