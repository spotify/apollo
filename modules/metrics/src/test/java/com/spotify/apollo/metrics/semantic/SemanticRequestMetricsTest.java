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

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import com.spotify.apollo.Request;
import com.spotify.apollo.Response;
import com.spotify.metrics.core.MetricId;
import com.spotify.metrics.core.SemanticMetricRegistry;

import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import okio.ByteString;

import static com.spotify.apollo.Status.FOUND;
import static com.spotify.apollo.Status.INTERNAL_SERVER_ERROR;
import static com.spotify.apollo.metrics.semantic.Metric.DROPPED_REQUEST_RATE;
import static com.spotify.apollo.metrics.semantic.Metric.ENDPOINT_REQUEST_DURATION;
import static com.spotify.apollo.metrics.semantic.Metric.ENDPOINT_REQUEST_RATE;
import static com.spotify.apollo.metrics.semantic.Metric.ERROR_RATIO;
import static com.spotify.apollo.metrics.semantic.Metric.RESPONSE_PAYLOAD_SIZE;
import static com.spotify.apollo.metrics.semantic.Metric.REQUEST_FANOUT_FACTOR;
import static com.spotify.apollo.metrics.semantic.Metric.REQUEST_PAYLOAD_SIZE;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.iterableWithSize;

public class SemanticRequestMetricsTest {

  private SemanticMetricRegistry metricRegistry;
  private SemanticRequestMetrics sut;

  @Before
  public void setUp() throws Exception {
    sut = semanticRequestMetrics(EnumSet.allOf(Metric.class));
  }

  private SemanticRequestMetrics semanticRequestMetrics(EnumSet<Metric> enabledMetrics) {
    metricRegistry = new SemanticMetricRegistry();

    return new SemanticRequestMetrics(
        enabledMetrics,
        metricRegistry,
        MetricId.EMPTY.tagged("service", "test-service",
                              "endpoint", "hm://foo/<bar>"),
        new Meter(),
        new Meter());
  }

  @Test
  public void shouldTrackFanout() throws Exception {

    sut.fanout(29);

    assertThat(
        metricRegistry.getMetrics(),
        hasKey(
            hasProperty("tags", allOf(
                hasEntry("service", "test-service"),
                hasEntry("component", "service-request"),
                hasEntry("what", "request-fanout-factor"),
                hasEntry("endpoint", "hm://foo/<bar>"),
                hasEntry("unit", "request/request")
            ))
        )
    );
  }

  @Test
  public void shouldTrackRequestRate() throws Exception {
    sut.response(Response.forStatus(FOUND));

    assertThat(
        metricRegistry.getMetrics(),
        hasKey(
            hasProperty("tags", allOf(
                hasEntry("service", "test-service"),
                hasEntry("component", "service-request"),
                hasEntry("what", "endpoint-request-rate"),
                hasEntry("endpoint", "hm://foo/<bar>"),
                hasEntry("status-code", "302"),
                hasEntry("unit", "request")
            ))
        )
    );
  }

  @Test
  public void shouldTrackRequestDuration() throws Exception {
    assertThat(
        metricRegistry.getMetrics(),
        hasKey(
            hasProperty("tags", allOf(
                hasEntry("service", "test-service"),
                hasEntry("component", "service-request"),
                hasEntry("what", "endpoint-request-duration"),
                hasEntry("endpoint", "hm://foo/<bar>")
            ))
        )
    );
  }

  @Test
  public void shouldCalculateRequestDurationOnResponse() throws Exception {

    sut.response(Response.ok());

    Collection<Timer> timers = metricRegistry.getTimers(
        (metricId, metric) ->
            metricId.getTags().get("what").equals("endpoint-request-duration")
    ).values();

    assertThat(timers.size(), is(1));
    assertThat(timers.iterator().next().getCount(), is(1L));
  }

  @Test
  public void shouldCalculateRequestDurationOnDrop() throws Exception {

    sut.drop();

    Collection<Timer> timers = metricRegistry.getTimers(
        (metricId, metric) ->
            metricId.getTags().get("what").equals("endpoint-request-duration")
    ).values();

    assertThat(timers.size(), is(1));
    assertThat(timers.iterator().next().getCount(), is(1L));
  }

  @Test
  public void shouldTrackOneMinErrorRatio() throws Exception {
    sut.response(Response.ok());

    assertThat(metricRegistry.getGauges(
        (metricId, metric) ->
            metricId.getTags().get("what").equals("error-ratio") &&
            metricId.getTags().get("stat").equals("1m"))
                   .values(),
               iterableWithSize(1));
  }

  @Test
  public void shouldTrackFiveMinErrorRatio() throws Exception {
    sut.response(Response.ok());

    assertThat(metricRegistry.getGauges(
        (metricId, metric) ->
            metricId.getTags().get("what").equals("error-ratio") &&
            metricId.getTags().get("stat").equals("5m"))
                   .values(),
               iterableWithSize(1));
  }

  @Test
  public void shouldTrackFifteenMinErrorRatio() throws Exception {
    sut.response(Response.ok());

    assertThat(metricRegistry.getGauges(
        (metricId, metric) ->
            metricId.getTags().get("what").equals("error-ratio") &&
            metricId.getTags().get("stat").equals("15m"))
                   .values(),
               iterableWithSize(1));
  }

  @Test
  public void shouldCalculateOneMinErrorRatio() throws Exception {
    sut.response(Response.ok());
    sut.response(Response.forStatus(INTERNAL_SERVER_ERROR));

    //noinspection OptionalGetWithoutIsPresent
    // the test above will fail if there's not exactly 1 such element
    Gauge oneMin = metricRegistry.getGauges(
        (metricId, metric) ->
            metricId.getTags().get("what").equals("error-ratio") &&
            metricId.getTags().get("stat").equals("1m"))
        .values().stream().findFirst().get();

    // semantic metrics Meters take some time to update
    await().atMost(15, TimeUnit.SECONDS).until(() -> (Double) oneMin.getValue() > 0.3 && (Double) oneMin.getValue() < 0.7);
  }

  @Test
  public void shouldCountDroppedRequests() throws Exception {
    sut.drop();

    Collection<Meter> meters = metricRegistry.getMeters(
        (metricId, metric) ->
            metricId.getTags().get("what").equals("dropped-request-rate")
    ).values();

    assertThat(meters.size(), is(1));
    assertThat(meters.iterator().next().getCount(), is(1L));
  }

  @Test
  public void shouldCalculateResponseSizes() throws Exception {
    sut.response(Response.forPayload(ByteString.encodeUtf8("this has non-zero size")));

    Collection<Histogram> histograms = metricRegistry.getHistograms(
        (metricId, metric) ->
            metricId.getTags().get("what").equals("response-payload-size") &&
            metricId.getTags().get("unit").equals("B")
    ).values();

    assertThat(histograms, iterableWithSize(1));
    assertThat(histograms.iterator().next().getCount(), is(1L));
  }

  @Test
  public void shouldCalculateRequestSizes() throws Exception {
    sut.incoming(Request.forUri("hm://foo").withPayload(ByteString.encodeUtf8("small, but nice")));

    Collection<Histogram> histograms = metricRegistry.getHistograms(
        (metricId, metric) ->
            metricId.getTags().get("what").equals("request-payload-size") &&
            metricId.getTags().get("unit").equals("B")
    ).values();

    assertThat(histograms, iterableWithSize(1));
    assertThat(histograms.iterator().next().getCount(), is(1L));
  }

  @Test
  public void shouldSupportDisablingFanout() throws Exception {
    sut = semanticRequestMetrics(EnumSet.complementOf(EnumSet.of(REQUEST_FANOUT_FACTOR)));

    sut.fanout(3240);

    assertNotInRegistry(REQUEST_FANOUT_FACTOR);
  }

  @Test
  public void shouldSupportDisablingRequestRate() throws Exception {
    sut = semanticRequestMetrics(EnumSet.complementOf(EnumSet.of(ENDPOINT_REQUEST_RATE)));

    sut.response(Response.ok());

    assertNotInRegistry(ENDPOINT_REQUEST_RATE);
  }

  @Test
  public void shouldSupportDisablingRequestDuration() throws Exception {
    sut = semanticRequestMetrics(EnumSet.complementOf(EnumSet.of(ENDPOINT_REQUEST_DURATION)));

    sut.response(Response.ok());

    assertNotInRegistry(ENDPOINT_REQUEST_DURATION);
  }

  @Test
  public void shouldSupportDisablingDropRate() throws Exception {
    sut = semanticRequestMetrics(EnumSet.complementOf(EnumSet.of(DROPPED_REQUEST_RATE)));

    sut.drop();

    assertNotInRegistry(DROPPED_REQUEST_RATE);
  }

  @Test
  public void shouldSupportDisablingErrorRatio() throws Exception {
    sut = semanticRequestMetrics(EnumSet.complementOf(EnumSet.of(ERROR_RATIO)));

    sut.response(Response.ok());

    assertNotInRegistry(ERROR_RATIO);
  }

  @Test
  public void shouldSupportDisablingRequestSize() throws Exception {
    sut = semanticRequestMetrics(EnumSet.complementOf(EnumSet.of(REQUEST_PAYLOAD_SIZE)));

    sut.response(Response.forPayload(ByteString.encodeUtf8("flop")));

    assertNotInRegistry(REQUEST_PAYLOAD_SIZE);
  }

  @Test
  public void shouldSupportDisablingResponseSize() throws Exception {
    sut = semanticRequestMetrics(EnumSet.complementOf(EnumSet.of(RESPONSE_PAYLOAD_SIZE)));

    sut.response(Response.forPayload(ByteString.encodeUtf8("flop")));

    assertNotInRegistry(RESPONSE_PAYLOAD_SIZE);
  }

  private void assertNotInRegistry(Metric metric) {
    Optional<MetricId> metricId = metricRegistry.getNames().stream().
        filter(id -> id.getTags().get("what").equals(metric.what()))
        .findAny();

    assertThat(metricId, is(Optional.empty()));
  }
}
