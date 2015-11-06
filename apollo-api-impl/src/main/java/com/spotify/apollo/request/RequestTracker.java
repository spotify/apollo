/**
 * Copyright (C) 2013 Spotify AB
 */

package com.spotify.apollo.request;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Maps.newConcurrentMap;
import static com.google.common.util.concurrent.Futures.getUnchecked;
import static com.spotify.apollo.request.TrackedOngoingRequest.FailureCause.TIMEOUT;

public class RequestTracker implements Closeable {

  private static final Logger LOG = LoggerFactory.getLogger(RequestTracker.class);

  private static final ScheduledExecutorService TRACKER_EXECUTOR =
      Executors.newSingleThreadScheduledExecutor(
          new ThreadFactoryBuilder()
              .setDaemon(true)
              .setNameFormat("apollo-request-reaper")
              .build());

  // TODO: this should be sorted out to match whatever we decide to do with message id
  private final ConcurrentMap<TrackedOngoingRequest, TrackedOngoingRequest> outstanding = newConcurrentMap();

  private final ScheduledFuture<?> future;

  public RequestTracker() {
    // As the number of outstanding requests is relatively small, it is cheaper to simply
    // regularly iterate over all outstanding requests instead of scheduling callbacks.
    this.future = TRACKER_EXECUTOR.scheduleWithFixedDelay(this::reap, 10, 10, TimeUnit.MILLISECONDS);
  }

  public void register(TrackedOngoingRequest request) {
    outstanding.put(request, request);
  }

  public boolean remove(TrackedOngoingRequest request) {
    return this.outstanding.remove(request) != null;
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
    outstanding.values().stream()
        // Drop expired requests
        .filter(TrackedOngoingRequest::isExpired)
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
    final Set<TrackedOngoingRequest> requests = ImmutableSet.copyOf(outstanding.keySet());
    for (TrackedOngoingRequest id : requests) {
      final TrackedOngoingRequest removed = outstanding.remove(id);
      if (removed != null) {
        removed.fail(TIMEOUT);
      }
    }
  }
}
