/*
 * -\-\-
 * Spotify Apollo Metrics Module
 * --
 * Copyright (C) 2013 - 2018 Spotify AB
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.codahale.metrics.Histogram;
import com.spotify.apollo.Response;
import com.spotify.apollo.metrics.RequestMetrics;
import com.spotify.metrics.core.MetricId;
import com.spotify.metrics.core.ReservoirWithTtl;
import com.spotify.metrics.core.SemanticMetricRegistry;
import com.typesafe.config.ConfigFactory;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import okio.ByteString;
import org.junit.Before;
import org.junit.Test;

public class HistogramExpirationTest {
  private static int RESERVOIR_TTL_SECONDS = 1;

  private SemanticMetricRegistry metricRegistry;

  private RequestMetrics requestMetrics;

  @Before
  public void setUp() {
    metricRegistry = new SemanticMetricRegistry(() -> new ReservoirWithTtl(RESERVOIR_TTL_SECONDS));

    SemanticServiceMetrics serviceMetrics = new SemanticServiceMetrics(
      metricRegistry,
      MetricId.EMPTY,
      Collections.emptySet(),
      what -> true,
      DurationThresholdConfig.parseConfig(ConfigFactory.empty())
    );

    requestMetrics = serviceMetrics.metricsForEndpointCall("GET:/bar");
  }

  @Test
  public void shouldExpireOldValues() throws InterruptedException {
    requestMetrics.response(Response.forPayload(ByteString.of("huge-payload!".getBytes())));

    final Optional<Histogram> payloadSize = metricRegistry.getHistograms().entrySet().stream()
      .filter((entry) -> entry.getKey().getTags().get("what").equals("response-payload-size"))
      .map(Map.Entry::getValue)
      .findFirst();

    assertThat(payloadSize.isPresent(), is(true));
    assertThat(payloadSize.get().getSnapshot().get99thPercentile(), is(13D));
    // Sucks that we have to sleep in a unit test but I can't think of another way to test this.
    Thread.sleep(1500L);
    assertThat(payloadSize.get().getSnapshot().get99thPercentile(), is(0D));
  }
}
