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

import com.spotify.metrics.core.MetricId;
import com.spotify.metrics.core.SemanticMetricRegistry;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasProperty;

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
        metricRegistry.getMetrics().keySet(),
        contains(
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
        metricRegistry.getMetrics().keySet(),
        contains(
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
        metricRegistry.getMetrics().keySet(),
        contains(
            hasProperty("tags", allOf(
                hasEntry("service", "test-service"),
                hasEntry("component", "service-request"),
                hasEntry("what", "endpoint-request-duration"),
                hasEntry("endpoint", "hm://foo/<bar>")
            ))
        )
    );
  }
}
