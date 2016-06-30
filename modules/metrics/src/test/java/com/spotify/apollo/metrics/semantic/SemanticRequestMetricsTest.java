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
import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import com.spotify.apollo.Response;
import com.spotify.metrics.core.MetricId;
import com.spotify.metrics.core.SemanticMetricRegistry;

import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import static com.spotify.apollo.Status.FOUND;
import static com.spotify.apollo.Status.INTERNAL_SERVER_ERROR;
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
    metricRegistry = new SemanticMetricRegistry();
    sut = new SemanticRequestMetrics(
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
  public void shouldTrackRequestStatusCode() throws Exception {

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
}
