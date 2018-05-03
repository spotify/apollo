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

import com.codahale.metrics.Meter;
import com.spotify.metrics.core.MetricId;
import com.spotify.metrics.core.SemanticMetricRegistry;

public class DurationThresholdTracker {

  private final Meter durationThresholdMeter;
  private final Integer threshold;

  public DurationThresholdTracker(MetricId id, SemanticMetricRegistry
      metricRegistry, Integer threshold) {
    final MetricId thresholdId = id.tagged("what", ENDPOINT_REQUEST_DURATION_THRESHOLD_RATE.tag())
        .tagged("threshold", threshold.toString());
    this.durationThresholdMeter = metricRegistry.meter(thresholdId);
    this.threshold = threshold;
  }

  /**
   * Compares the duration of the current request (milliseconds) to the a
   * threshold goal and tracks how many requests meet this goal.
   *
   * @param duration - the duration of the current request
   */
  public void markDurationThresholds(final long duration) {
    if (duration <= threshold) {
      durationThresholdMeter.mark();
    }
  }
}
