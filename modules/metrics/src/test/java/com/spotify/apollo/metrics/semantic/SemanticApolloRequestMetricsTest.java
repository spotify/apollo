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
import com.spotify.metrics.core.MetricId;
import com.spotify.metrics.core.SemanticMetricRegistry;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.iterableWithSize;

public class SemanticApolloRequestMetricsTest {

  private SemanticMetricRegistry metricRegistry;
  private SemanticApolloRequestMetrics sut;

  @Before
  public void setUp() throws Exception {
    metricRegistry = new SemanticMetricRegistry();
    sut = new SemanticApolloRequestMetrics(
        metricRegistry,
        MetricId.EMPTY.tagged("service", "test-service",
                              "endpoint", "hm://foo/<bar>"));
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

    sut.countRequest(302);

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

    sut.timeRequest().stop();

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
  public void shouldTrackOneMinErrorRatio() throws Exception {
    sut.countRequest(200);

    assertThat(metricRegistry.getGauges(
        (metricId, metric) ->
            metricId.getTags().get("what").equals("error-ratio") &&
            metricId.getTags().get("stat").equals("1m"))
                   .values(),
               iterableWithSize(1));
  }

  @Test
  public void shouldTrackFiveMinErrorRatio() throws Exception {
    sut.countRequest(200);

    assertThat(metricRegistry.getGauges(
        (metricId, metric) ->
            metricId.getTags().get("what").equals("error-ratio") &&
            metricId.getTags().get("stat").equals("5m"))
                   .values(),
               iterableWithSize(1));
  }

  @Test
  public void shouldTrackFifteenMinErrorRatio() throws Exception {
    sut.countRequest(200);

    assertThat(metricRegistry.getGauges(
        (metricId, metric) ->
            metricId.getTags().get("what").equals("error-ratio") &&
            metricId.getTags().get("stat").equals("15m"))
                   .values(),
               iterableWithSize(1));
  }

  @Test
  public void shouldCalculateOneMinErrorRatio() throws Exception {
    sut.countRequest(200);
    sut.countRequest(500);

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
}
