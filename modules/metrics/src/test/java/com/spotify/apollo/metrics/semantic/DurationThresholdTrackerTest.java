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

import static com.spotify.apollo.metrics.semantic.What.ENDPOINT_REQUEST_DURATION_THRESHOLD_RATE;
import static org.junit.Assert.assertEquals;

import com.codahale.metrics.Meter;
import com.spotify.metrics.core.MetricId;
import com.spotify.metrics.core.SemanticMetricRegistry;
import org.junit.Before;
import org.junit.Test;

public class DurationThresholdTrackerTest {
  private DurationThresholdTracker durationThresholdTracker;
  private SemanticMetricRegistry registry;
  private MetricId metricId;

  @Before
  public void setUp() throws Exception {
    registry = new SemanticMetricRegistry();
    metricId = MetricId.build();
    durationThresholdTracker = new DurationThresholdTracker(metricId, registry, 50);
  }

  @Test
  public void markDurationThresholds() throws Exception {
    final Meter meter = registry.getMeters().get(metricId.tagged("what",
        ENDPOINT_REQUEST_DURATION_THRESHOLD_RATE.tag()).tagged("threshold", "50"));
    durationThresholdTracker.markDurationThresholds(20);
    durationThresholdTracker.markDurationThresholds(50);
    durationThresholdTracker.markDurationThresholds(100); // should not affect count
    final long count = meter.getCount();
    assertEquals(2, count);
  }

}
