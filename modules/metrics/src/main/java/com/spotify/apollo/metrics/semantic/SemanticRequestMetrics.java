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

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import com.spotify.apollo.Request;
import com.spotify.apollo.Response;
import com.spotify.apollo.StatusType;
import com.spotify.apollo.metrics.RequestMetrics;

import java.util.Optional;
import java.util.function.Consumer;

import okio.ByteString;

import static com.spotify.apollo.StatusType.Family.INFORMATIONAL;
import static com.spotify.apollo.StatusType.Family.SUCCESSFUL;
import static java.util.Objects.requireNonNull;

// Optional fields are fine; they enable the use of the 'ifPresent' idiom which is more readable
// than if statements
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
class SemanticRequestMetrics implements RequestMetrics {

  private final Optional<Consumer<Response<ByteString>>> requestRateCounter;
  private final Optional<Histogram> fanoutHistogram;
  private final Optional<Histogram> requestSizeHistogram;
  private final Optional<Histogram> responseSizeHistogram;
  private final Optional<Timer.Context> timerContext;
  private final Optional<Meter> droppedRequests;
  private final Meter sentReplies;
  private final Meter sentErrors;

  SemanticRequestMetrics(
      Optional<Consumer<Response<ByteString>>> requestRateCounter,
      Optional<Histogram> fanoutHistogram,
      Optional<Histogram> responseSizeHistogram,
      Optional<Histogram> requestSizeHistogram,
      Optional<Timer.Context> timerContext,
      Optional<Meter> droppedRequests,
      Meter sentReplies,
      Meter sentErrors) {

    this.requestRateCounter = requireNonNull(requestRateCounter);
    this.fanoutHistogram = requireNonNull(fanoutHistogram);
    this.responseSizeHistogram = requireNonNull(responseSizeHistogram);
    this.requestSizeHistogram = requireNonNull(requestSizeHistogram);
    this.timerContext = requireNonNull(timerContext);
    this.droppedRequests = requireNonNull(droppedRequests);
    this.sentReplies = requireNonNull(sentReplies);
    this.sentErrors = requireNonNull(sentErrors);
  }

  @Override
  public void incoming(Request request) {
    requestSizeHistogram
        .ifPresent(histogram -> request.payload()
            .ifPresent(payload -> histogram.update(payload.size())));
  }

  @Override
  public void fanout(int requests) {
    fanoutHistogram.ifPresent(histogram -> histogram.update(requests));
  }

  @Override
  public void response(Response<ByteString> response) {
    requestRateCounter.ifPresent(consumer -> consumer.accept(response));
    responseSizeHistogram
        .ifPresent(histogram -> response.payload()
            .ifPresent(payload -> histogram.update(payload.size())));

    sentReplies.mark();
    timerContext.ifPresent(Timer.Context::stop);

    StatusType.Family family = response.status().family();
    if (family != INFORMATIONAL && family != SUCCESSFUL) {
      sentErrors.mark();
    }
  }

  @Override
  public void drop() {
    droppedRequests.ifPresent(Meter::mark);
    timerContext.ifPresent(Timer.Context::stop);
  }
}
