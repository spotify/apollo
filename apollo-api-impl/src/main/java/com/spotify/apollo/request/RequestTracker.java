/*
 * -\-\-
 * Spotify Apollo API Implementations
 * --
 * Copyright (C) 2013 - 2015 Spotify AB
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
package com.spotify.apollo.request;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import com.spotify.apollo.Response;
import com.spotify.apollo.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.google.common.util.concurrent.Futures.getUnchecked;

public class RequestTracker implements Closeable {

  private static final Logger LOG = LoggerFactory.getLogger(RequestTracker.class);

  private static final ScheduledExecutorService TRACKER_EXECUTOR =
      Executors.newSingleThreadScheduledExecutor(
          new ThreadFactoryBuilder()
              .setDaemon(true)
              .setNameFormat("apollo-request-reaper")
              .build());

  private final Set<OngoingRequest> outstanding = ConcurrentHashMap.newKeySet();

  private final ScheduledFuture<?> future;

  public RequestTracker() {
    // As the number of outstanding requests is relatively small, it is cheaper to simply
    // regularly iterate over all outstanding requests instead of scheduling callbacks.
    this.future = TRACKER_EXECUTOR.scheduleWithFixedDelay(this::reap, 10, 10, TimeUnit.MILLISECONDS);
  }

  public void register(OngoingRequest request) {
    outstanding.add(request);
  }

  public boolean remove(OngoingRequest request) {
    return this.outstanding.remove(request);
  }

  @Override
  public void close() {
    future.cancel(false);
    try {
      getUnchecked(future);
    } catch (CancellationException e) {
      // ignore
    }

    failRequests();
  }

  @VisibleForTesting
  void reap() {
    outstanding.stream()
        // Drop expired requests
        .filter(OngoingRequest::isExpired)
        .forEach(
            request -> {
              LOG.warn("Dropping expired request: {}", request);
              request.drop();
            });
  }

  /**
   * Fail all outstanding requests.
   */
  private void failRequests() {
    final Set<OngoingRequest> requests = ImmutableSet.copyOf(outstanding);
    for (OngoingRequest id : requests) {
      final boolean removed = outstanding.remove(id);
      if (removed) {
        id.reply(Response.forStatus(Status.SERVICE_UNAVAILABLE));
      }
    }
  }
}
