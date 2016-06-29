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
package com.spotify.apollo.metrics;

import com.spotify.apollo.StatusType;

/**
 * Defines an interface for how to collect statistics for an individual incoming request.
 */
public interface RequestMetrics {

  /**
   * Register the fanout factor.
   *
   * @param requestsMade the number of requests made to other services for this incoming request
   */
  void fanout(int requestsMade);

  /**
   * Register the response for this request - should be invoked once a reply is available.
   *
   * @param status the response status
   */
  void responseStatus(StatusType status);

  /**
   * Starts a timer for the request and returns a {@link TimerContext} that allows a caller to stop
   * the timer once the request has finished processing.
   */
  TimerContext timeRequest();
}
