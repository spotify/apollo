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

import com.spotify.apollo.Response;
import com.spotify.apollo.request.ForwardingOngoingRequest;
import com.spotify.apollo.request.OngoingRequest;
import com.spotify.apollo.request.TrackedOngoingRequest;

import java.util.concurrent.atomic.AtomicInteger;

import okio.ByteString;

import static java.util.Objects.requireNonNull;

class MetricsTrackingOngoingRequest
    extends ForwardingOngoingRequest
    implements TrackedOngoingRequest {

  private final ApolloRequestMetrics metrics;
  private final ApolloTimerContext timerContext;

  private final AtomicInteger requestCounter = new AtomicInteger();

  MetricsTrackingOngoingRequest(
      ApolloRequestMetrics metrics,
      OngoingRequest ongoingRequest,
      ApolloTimerContext timerContext) {
    super(ongoingRequest);
    this.metrics = metrics;

    this.timerContext = requireNonNull(timerContext);
  }

  @Override
  public void reply(Response<ByteString> message) {
    metrics.fanout(requestCounter.get());
    metrics.countRequest(message.status().code());
    timerContext.stop();
    super.reply(message);
  }

  @Override
  public void drop() {
    timerContext.stop();
    super.drop();
  }

  /**
   * Increment number of downstream calls associated with this request.
   */
  @Override
  public void incrementDownstreamRequests() {
    requestCounter.incrementAndGet();
  }
}
