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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableSet;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.RatioGauge;
import com.codahale.metrics.RatioGauge.Ratio;
import com.codahale.metrics.Timer;
import com.spotify.apollo.Response;
import com.spotify.apollo.metrics.RequestMetrics;
import com.spotify.apollo.metrics.ServiceMetrics;
import com.spotify.metrics.core.MetricId;
import com.spotify.metrics.core.SemanticMetricRegistry;

import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import okio.ByteString;

import static com.spotify.apollo.metrics.semantic.What.DROPPED_REQUEST_RATE;
import static com.spotify.apollo.metrics.semantic.What.ENDPOINT_REQUEST_DURATION;
import static com.spotify.apollo.metrics.semantic.What.ENDPOINT_REQUEST_RATE;
import static com.spotify.apollo.metrics.semantic.What.ERROR_RATIO;
import static com.spotify.apollo.metrics.semantic.What.ERROR_RATIO_4XX;
import static com.spotify.apollo.metrics.semantic.What.ERROR_RATIO_5XX;
import static com.spotify.apollo.metrics.semantic.What.REQUEST_FANOUT_FACTOR;
import static com.spotify.apollo.metrics.semantic.What.REQUEST_PAYLOAD_SIZE;
import static com.spotify.apollo.metrics.semantic.What.RESPONSE_PAYLOAD_SIZE;
import static java.lang.Math.max;
import static java.util.Objects.requireNonNull;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
class SemanticServiceMetrics implements ServiceMetrics {

  // Minimum reply rate at which we consider the error ratio to make sense.
  private static final double ERROR_GAUGE_MINIMUM_REPLY_RATE = 1e-3;
  // If a ratio is below this value, report it as zero
  private static final double ERROR_RATIO_MINIMUM = 1e-15;

  private final SemanticMetricRegistry metricRegistry;
  private final MetricId metricId;
  private final Predicate<What> enabledMetrics;
  private final Set<Integer> precreateCodes;
  private final LoadingCache<String, CachedMeters> metersCache;

  SemanticServiceMetrics(SemanticMetricRegistry metricRegistry,
                         MetricId id,
                         Set<Integer> precreateCodes,
                         Predicate<What> enabledMetrics) {
    this.metricRegistry = requireNonNull(metricRegistry);
    // Already tagged with 'application' and 'service'
    this.metricId = requireNonNull(id);
    this.enabledMetrics = requireNonNull(enabledMetrics);
    this.precreateCodes = ImmutableSet.copyOf(precreateCodes);

    metersCache = CacheBuilder.<String, CachedMeters>newBuilder()
        .build(new CacheLoader<String, CachedMeters>() {
          @Override
          public CachedMeters load(String endpoint) throws Exception {
            return metersForEndpoint(endpoint);
          }
        });
  }

  @Override
  public RequestMetrics metricsForEndpointCall(String endpoint) {
    CachedMeters meters = metersCache.getUnchecked(endpoint);

    return new SemanticRequestMetrics(
        meters.requestRateCounter,
        meters.fanoutHistogram,
        meters.responseSizeHistogram,
        meters.requestSizeHistogram,
        meters.requestDurationTimer.map(Timer::time),
        meters.droppedRequests,
        meters.sentReplies,
        meters.sentErrors,
        meters.sentErrors4xx,
        meters.sentErrors5xx);
  }

  private CachedMeters metersForEndpoint(String endpoint) {
    MetricId id = metricId.tagged("endpoint", endpoint);

    // precreate meters for defined status codes to ensure that they start out at value 0 on restart
    for (Integer code : precreateCodes) {
      requestRateMeter(id, code);
    }

    Meter sentReplies = new Meter();
    Meter sentErrors = new Meter();
    Meter sentErrors4xx = new Meter();
    Meter sentErrors5xx = new Meter();

    if (enabledMetrics.test(ERROR_RATIO)) {
      registerRatioGauge(id, "1m", errorRatioSupplier(sentErrors::getOneMinuteRate,
                                                      sentReplies::getOneMinuteRate),
                         metricRegistry,
                         ERROR_RATIO);
      registerRatioGauge(id, "5m", errorRatioSupplier(sentErrors::getFiveMinuteRate,
                                                      sentReplies::getFiveMinuteRate),
                         metricRegistry,
                         ERROR_RATIO);
      registerRatioGauge(id, "15m", errorRatioSupplier(sentErrors::getFifteenMinuteRate,
                                                       sentReplies::getFifteenMinuteRate),
                         metricRegistry,
                         ERROR_RATIO);
    }
    if (enabledMetrics.test(ERROR_RATIO_4XX)) {
      registerRatioGauge(id, "1m", errorRatioSupplier(sentErrors4xx::getOneMinuteRate,
                                                      sentReplies::getOneMinuteRate),
                         metricRegistry,
                         ERROR_RATIO_4XX);
      registerRatioGauge(id, "5m", errorRatioSupplier(sentErrors4xx::getFiveMinuteRate,
                                                      sentReplies::getFiveMinuteRate),
                         metricRegistry,
                         ERROR_RATIO_4XX);
      registerRatioGauge(id, "15m", errorRatioSupplier(sentErrors4xx::getFifteenMinuteRate,
                                                       sentReplies::getFifteenMinuteRate),
                         metricRegistry,
                         ERROR_RATIO_4XX);
    }
    if (enabledMetrics.test(ERROR_RATIO_5XX)) {
      registerRatioGauge(id, "1m", errorRatioSupplier(sentErrors5xx::getOneMinuteRate,
                                                      sentReplies::getOneMinuteRate),
                         metricRegistry,
                         ERROR_RATIO_5XX);
      registerRatioGauge(id, "5m", errorRatioSupplier(sentErrors5xx::getFiveMinuteRate,
                                                      sentReplies::getFiveMinuteRate),
                         metricRegistry,
                         ERROR_RATIO_5XX);
      registerRatioGauge(id, "15m", errorRatioSupplier(sentErrors5xx::getFifteenMinuteRate,
                                                       sentReplies::getFifteenMinuteRate),
                         metricRegistry,
                         ERROR_RATIO_5XX);
    }

    return new CachedMeters(
        requestRateCounter(id),
        fanoutHistogram(id),
        responseSizeHistogram(id),
        requestSizeHistogram(id),
        requestDurationTimer(id),
        droppedRequests(id),
        sentReplies,
        sentErrors,
        sentErrors4xx,
        sentErrors5xx);
  }

  private Optional<Meter> droppedRequests(MetricId id) {
    return enabledMetrics.test(DROPPED_REQUEST_RATE) ?
           Optional.of(metricRegistry.meter(
               id.tagged(
                   "what", DROPPED_REQUEST_RATE.tag(),
                   "unit", "request"
               ))) :
           Optional.empty();
  }

  private Optional<Timer> requestDurationTimer(MetricId id) {
    return enabledMetrics.test(ENDPOINT_REQUEST_DURATION) ?
           Optional.of(metricRegistry
                           .timer(id.tagged("what", ENDPOINT_REQUEST_DURATION.tag()))) :
           Optional.empty();
  }

  private Optional<Histogram> requestSizeHistogram(MetricId id) {
    return enabledMetrics.test(REQUEST_PAYLOAD_SIZE) ?
           Optional.of(metricRegistry.histogram(
               id.tagged(
                   "what", REQUEST_PAYLOAD_SIZE.tag(),
                   "unit", "B"
               ))) :
           Optional.empty();
  }

  private Optional<Histogram> responseSizeHistogram(MetricId id) {
    return enabledMetrics.test(RESPONSE_PAYLOAD_SIZE) ?
           Optional.of(metricRegistry.histogram(
               id.tagged(
                   "what", RESPONSE_PAYLOAD_SIZE.tag(),
                   "unit", "B"
               ))) :
           Optional.empty();
  }

  private Optional<Histogram> fanoutHistogram(MetricId id) {
    return enabledMetrics.test(REQUEST_FANOUT_FACTOR) ?
           Optional.of(metricRegistry.histogram(
               id.tagged(
                   "what", REQUEST_FANOUT_FACTOR.tag(),
                   "unit", "request/request"))) :
           Optional.empty();
  }

  private Optional<Consumer<Response<ByteString>>> requestRateCounter(MetricId id) {
    return enabledMetrics.test(ENDPOINT_REQUEST_RATE) ?
           Optional.of(response -> requestRateMeter(id, response.status().code()).mark()) :
           Optional.empty();
  }

  private Meter requestRateMeter(MetricId id, int code) {
    return metricRegistry
        .meter(id.tagged(
            "what", ENDPOINT_REQUEST_RATE.tag(),
            "unit", "request",
            "status-code", String.valueOf(code)));
  }

  private void registerRatioGauge(MetricId metricId,
                                  String stat,
                                  Supplier<Ratio> ratioSupplier,
                                  SemanticMetricRegistry metricRegistry,
                                  What what) {
    metricRegistry.register(
        metricId.tagged("what", what.tag(), "stat", stat),
        new RatioGauge() {
          @Override
          protected Ratio getRatio() {
            return ratioSupplier.get();
          }
        });
  }

  private Supplier<Ratio> errorRatioSupplier(Supplier<Double> errorRateSupplier,
                                             Supplier<Double> replyRateSupplier) {
    // We limit both the denominator and the resulting ratio when calculating error ratios.
    //
    // The ratio is limited in order to avoid producing ratios that are arbitrarily close to zero
    // but not quite zero. Them being close to but not quite zero is not a problem per se but it
    // distorts the apparent meaning of graphs in some monitoring tools - when the ratio goes
    // from e.g. 1e-316 to 1e-300 it can look like a huge increase indicating that something
    // sinister is in the works even though the error ratio is basically nil.
    //
    // The denominator is limited to avoid noise in the ratio when the total reply rate is low,
    // dividing by a EWMA-computed rate can cause ratios that are higher than the true ratio due to
    // computational noise in the EWMA computations.
    // If a service is drained of traffic (for instance by moving traffic to another host or site),
    // the meter values will decay exponentially towards zero. If left for some time (hours),
    // rounding errors will cause the values of the meters to approach each other, thus skewing
    // the ratio. If an alert is tied to this ratio, it may trigger falsely. To fix this, we set a
    // minimum for the denominator in the ratio, effectively making sure it will be be bigger than
    // the error rate as both rates goes towards zero.
    return () -> {
      final double denominator = max(replyRateSupplier.get(), ERROR_GAUGE_MINIMUM_REPLY_RATE);
      final Ratio ratio = Ratio.of(errorRateSupplier.get(), denominator);
      return ratio.getValue() > ERROR_RATIO_MINIMUM ? ratio : Ratio.of(0, denominator);
    };
  }

  private static class CachedMeters {

    private final Optional<Consumer<Response<ByteString>>> requestRateCounter;
    private final Optional<Histogram> fanoutHistogram;
    private final Optional<Histogram> responseSizeHistogram;
    private final Optional<Histogram> requestSizeHistogram;
    private final Optional<Timer> requestDurationTimer;
    private final Optional<Meter> droppedRequests;
    private final Meter sentReplies;
    private final Meter sentErrors;
    private final Meter sentErrors4xx;
    private final Meter sentErrors5xx;


    private CachedMeters(Optional<Consumer<Response<ByteString>>> requestRateCounter,
                         Optional<Histogram> fanoutHistogram,
                         Optional<Histogram> responseSizeHistogram,
                         Optional<Histogram> requestSizeHistogram,
                         Optional<Timer> requestDurationTimer,
                         Optional<Meter> droppedRequests,
                         Meter sentReplies, Meter sentErrors,
                         Meter sentErrors4xx, Meter sentErrors5xx) {
      this.requestRateCounter = requestRateCounter;
      this.fanoutHistogram = fanoutHistogram;
      this.requestSizeHistogram = requestSizeHistogram;
      this.responseSizeHistogram = responseSizeHistogram;
      this.requestDurationTimer = requestDurationTimer;
      this.droppedRequests = droppedRequests;
      this.sentReplies = sentReplies;
      this.sentErrors = sentErrors;
      this.sentErrors4xx = sentErrors4xx;
      this.sentErrors5xx = sentErrors5xx;
    }
  }
}
