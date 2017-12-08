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

import com.google.common.collect.ImmutableSet;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import com.spotify.apollo.Request;
import com.spotify.apollo.Response;
import com.spotify.apollo.metrics.RequestMetrics;
import com.spotify.metrics.core.MetricId;
import com.spotify.metrics.core.SemanticMetricRegistry;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import okio.ByteString;

import static com.spotify.apollo.Status.FOUND;
import static com.spotify.apollo.Status.INTERNAL_SERVER_ERROR;
import static com.spotify.apollo.Status.BAD_REQUEST;
import static com.spotify.apollo.metrics.semantic.What.DROPPED_REQUEST_RATE;
import static com.spotify.apollo.metrics.semantic.What.ENDPOINT_REQUEST_DURATION;
import static com.spotify.apollo.metrics.semantic.What.ENDPOINT_REQUEST_RATE;
import static com.spotify.apollo.metrics.semantic.What.ERROR_RATIO;
import static com.spotify.apollo.metrics.semantic.What.REQUEST_FANOUT_FACTOR;
import static com.spotify.apollo.metrics.semantic.What.REQUEST_PAYLOAD_SIZE;
import static com.spotify.apollo.metrics.semantic.What.RESPONSE_PAYLOAD_SIZE;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.iterableWithSize;

public class SemanticServiceMetricsTest {

  private SemanticMetricRegistry metricRegistry;

  private RequestMetrics requestMetrics;

  @Before
  public void setUp() throws Exception {
    setupWithPredicate(what -> true);
  }

  private void setupWithPredicate(Predicate<What> predicate) {
    setupWith(predicate, Collections.emptySet());
  }

  private void setupWith(Predicate<What> predicate, Set<Integer> precreateCodes) {
    metricRegistry = new SemanticMetricRegistry();
    SemanticServiceMetrics serviceMetrics = new SemanticServiceMetrics(
        metricRegistry,
        MetricId.EMPTY
            .tagged("service", "test-service"),
        precreateCodes,
        predicate);

    requestMetrics = serviceMetrics.metricsForEndpointCall("hm://foo/<bar>");
  }

  @Test
  public void shouldTrackFanout() throws Exception {

    requestMetrics.fanout(29);

    assertThat(
        metricRegistry.getMetrics(),
        hasKey(
            hasProperty("tags", allOf(
                hasEntry("service", "test-service"),
                hasEntry("what", "request-fanout-factor"),
                hasEntry("endpoint", "hm://foo/<bar>"),
                hasEntry("unit", "request/request")
            ))
        )
    );
  }

  @Test
  public void shouldTrackRequestRate() throws Exception {
    requestMetrics.response(Response.forStatus(FOUND));

    assertThat(
        metricRegistry.getMetrics(),
        hasKey(
            hasProperty("tags", allOf(
                hasEntry("service", "test-service"),
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
                hasEntry("what", "endpoint-request-duration"),
                hasEntry("endpoint", "hm://foo/<bar>")
            ))
        )
    );
  }

  @Test
  public void shouldCalculateRequestDurationOnResponse() throws Exception {

    requestMetrics.response(Response.ok());

    Collection<Timer> timers = metricRegistry.getTimers(
        (metricId, metric) ->
            metricId.getTags().get("what").equals("endpoint-request-duration")
    ).values();

    assertThat(timers.size(), is(1));
    assertThat(timers.iterator().next().getCount(), is(1L));
  }

  @Test
  public void shouldCalculateRequestDurationOnDrop() throws Exception {

    requestMetrics.drop();

    Collection<Timer> timers = metricRegistry.getTimers(
        (metricId, metric) ->
            metricId.getTags().get("what").equals("endpoint-request-duration")
    ).values();

    assertThat(timers.size(), is(1));
    assertThat(timers.iterator().next().getCount(), is(1L));
  }

  @Test
  public void shouldTrackOneMinErrorRatio() throws Exception {
    requestMetrics.response(Response.ok());

    assertThat(metricRegistry.getGauges(
        (metricId, metric) ->
            metricId.getTags().get("what").equals("error-ratio") &&
            metricId.getTags().get("stat").equals("1m"))
                   .values(),
               iterableWithSize(1));
  }

  @Test
  public void shouldTrackFiveMinErrorRatio() throws Exception {
    requestMetrics.response(Response.ok());

    assertThat(metricRegistry.getGauges(
        (metricId, metric) ->
            metricId.getTags().get("what").equals("error-ratio") &&
            metricId.getTags().get("stat").equals("5m"))
                   .values(),
               iterableWithSize(1));
  }

  @Test
  public void shouldTrackFifteenMinErrorRatio() throws Exception {
    requestMetrics.response(Response.ok());

    assertThat(metricRegistry.getGauges(
        (metricId, metric) ->
            metricId.getTags().get("what").equals("error-ratio") &&
            metricId.getTags().get("stat").equals("15m"))
                   .values(),
               iterableWithSize(1));
  }

  @Test
  public void shouldTrackOneMinErrorRatio4xx() throws Exception {
    requestMetrics.response(Response.ok());

    assertThat(metricRegistry.getGauges(
        (metricId, metric) ->
            metricId.getTags().get("what").equals("error-ratio-4xx") &&
            metricId.getTags().get("stat").equals("1m"))
            .values(),
        iterableWithSize(1));
  }

  @Test
  public void shouldTrackFiveMinErrorRatio4xx() throws Exception {
    requestMetrics.response(Response.ok());

    assertThat(metricRegistry.getGauges(
        (metricId, metric) ->
            metricId.getTags().get("what").equals("error-ratio-4xx") &&
            metricId.getTags().get("stat").equals("5m"))
            .values(),
        iterableWithSize(1));
  }

  @Test
  public void shouldTrackFifteenMinErrorRatio4xx() throws Exception {
    requestMetrics.response(Response.ok());

    assertThat(metricRegistry.getGauges(
        (metricId, metric) ->
            metricId.getTags().get("what").equals("error-ratio-4xx") &&
            metricId.getTags().get("stat").equals("15m"))
            .values(),
        iterableWithSize(1));
  }

  @Test
  public void shouldTrackOneMinErrorRatio5xx() throws Exception {
    requestMetrics.response(Response.ok());

    assertThat(metricRegistry.getGauges(
        (metricId, metric) ->
            metricId.getTags().get("what").equals("error-ratio-5xx") &&
            metricId.getTags().get("stat").equals("1m"))
            .values(),
        iterableWithSize(1));
  }

  @Test
  public void shouldTrackFiveMinErrorRatio5xx() throws Exception {
    requestMetrics.response(Response.ok());

    assertThat(metricRegistry.getGauges(
        (metricId, metric) ->
            metricId.getTags().get("what").equals("error-ratio-5xx") &&
            metricId.getTags().get("stat").equals("5m"))
            .values(),
        iterableWithSize(1));
  }

  @Test
  public void shouldTrackFifteenMinErrorRatio5xx() throws Exception {
    requestMetrics.response(Response.ok());

    assertThat(metricRegistry.getGauges(
        (metricId, metric) ->
            metricId.getTags().get("what").equals("error-ratio-5xx") &&
            metricId.getTags().get("stat").equals("15m"))
            .values(),
        iterableWithSize(1));
  }

  @Test
  public void shouldCalculateOneMinErrorRatio() throws Exception {
    requestMetrics.response(Response.ok());
    requestMetrics.response(Response.forStatus(INTERNAL_SERVER_ERROR));

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
  public void shouldCalculateOneMinErrorRatio4xx() throws Exception {
    requestMetrics.response(Response.ok());
    requestMetrics.response(Response.forStatus(BAD_REQUEST));

    //noinspection OptionalGetWithoutIsPresent
    // the test above will fail if there's not exactly 1 such element
    Gauge oneMin = metricRegistry.getGauges(
        (metricId, metric) ->
            metricId.getTags().get("what").equals("error-ratio-4xx") &&
            metricId.getTags().get("stat").equals("1m"))
        .values().stream().findFirst().get();

    // semantic metrics Meters take some time to update
    await().atMost(15, TimeUnit.SECONDS).until(() -> (Double) oneMin.getValue() > 0.3 && (Double) oneMin.getValue() < 0.7);
  }

  @Test
  public void shouldCalculateOneMinErrorRatio5xx() throws Exception {
    requestMetrics.response(Response.ok());
    requestMetrics.response(Response.forStatus(INTERNAL_SERVER_ERROR));

    //noinspection OptionalGetWithoutIsPresent
    // the test above will fail if there's not exactly 1 such element
    Gauge oneMin = metricRegistry.getGauges(
        (metricId, metric) ->
            metricId.getTags().get("what").equals("error-ratio-5xx") &&
            metricId.getTags().get("stat").equals("1m"))
        .values().stream().findFirst().get();

    // semantic metrics Meters take some time to update
    await().atMost(15, TimeUnit.SECONDS).until(() -> (Double) oneMin.getValue() > 0.3 && (Double) oneMin.getValue() < 0.7);
  }

  @Test
  public void shouldCountDroppedRequests() throws Exception {
    requestMetrics.drop();

    Collection<Meter> meters = metricRegistry.getMeters(
        (metricId, metric) ->
            metricId.getTags().get("what").equals("dropped-request-rate")
    ).values();

    assertThat(meters.size(), is(1));
    assertThat(meters.iterator().next().getCount(), is(1L));
  }

  @Test
  public void shouldCalculateResponseSizes() throws Exception {
    requestMetrics.response(Response.forPayload(ByteString.encodeUtf8("this has non-zero size")));

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
    requestMetrics
        .incoming(Request.forUri("hm://foo").withPayload(ByteString.encodeUtf8("small, but nice")));

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
    setupWithPredicate(what -> what != REQUEST_FANOUT_FACTOR);

    requestMetrics.fanout(3240);

    assertNotInRegistry(REQUEST_FANOUT_FACTOR);
  }

  @Test
  public void shouldSupportDisablingRequestRate() throws Exception {
    setupWithPredicate(what -> what != ENDPOINT_REQUEST_RATE);

    requestMetrics.response(Response.ok());

    assertNotInRegistry(ENDPOINT_REQUEST_RATE);
  }

  @Test
  public void shouldSupportDisablingRequestDuration() throws Exception {
    setupWithPredicate(what -> what != ENDPOINT_REQUEST_DURATION);

    requestMetrics.response(Response.ok());

    assertNotInRegistry(ENDPOINT_REQUEST_DURATION);
  }

  @Test
  public void shouldSupportDisablingDropRate() throws Exception {
    setupWithPredicate(what -> what != DROPPED_REQUEST_RATE);

    requestMetrics.drop();

    assertNotInRegistry(DROPPED_REQUEST_RATE);
  }

  @Test
  public void shouldSupportDisablingErrorRatio() throws Exception {
    setupWithPredicate(what -> what != ERROR_RATIO);

    requestMetrics.response(Response.ok());

    assertNotInRegistry(ERROR_RATIO);
  }

  @Test
  public void shouldSupportDisablingRequestSize() throws Exception {
    setupWithPredicate(what -> what != REQUEST_PAYLOAD_SIZE);

    requestMetrics.response(Response.forPayload(ByteString.encodeUtf8("flop")));

    assertNotInRegistry(REQUEST_PAYLOAD_SIZE);
  }

  @Test
  public void shouldSupportDisablingResponseSize() throws Exception {
    setupWithPredicate(what -> what != RESPONSE_PAYLOAD_SIZE);

    requestMetrics.response(Response.forPayload(ByteString.encodeUtf8("flop")));

    assertNotInRegistry(RESPONSE_PAYLOAD_SIZE);
  }

  @Test
  public void shouldPrecreateMetersForDefinedStatusCodes() throws Exception {
    setupWith(what -> true, ImmutableSet.of(200, 503));

    Map<MetricId, Meter> meters = metricRegistry.getMeters(
        (metricId, metric) ->
            metricId.getTags().get("what").equals("endpoint-request-rate")
    );

    assertThat(meters.size(), is(2));
    assertThat(meters.keySet(), containsInAnyOrder(meterWithTag("status-code", "200"),
                                                   meterWithTag("status-code", "503")));
  }

  private Matcher<MetricId> meterWithTag(String tag, String value) {
    return new TypeSafeMatcher<MetricId>() {
      @Override
      protected boolean matchesSafely(MetricId item) {
        return value.equals(item.getTags().get(tag));
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("a metric Id with a tag '");
        description.appendText(tag);
        description.appendText("' whose value is '");
        description.appendText(value);
        description.appendText("'");
      }
    };
  }

  private void assertNotInRegistry(What metric) {
    Optional<MetricId> metricId = metricRegistry.getNames().stream().
        filter(id -> id.getTags().get("what").equals(metric.tag()))
        .findAny();

    assertThat(metricId, is(Optional.empty()));
  }
}