/**
 * Copyright (C) 2013 Spotify AB
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

  private final Set<TrackedOngoingRequest> outstanding = ConcurrentHashMap.newKeySet();

  private final ScheduledFuture<?> future;

  public RequestTracker() {
    // As the number of outstanding requests is relatively small, it is cheaper to simply
    // regularly iterate over all outstanding requests instead of scheduling callbacks.
    this.future = TRACKER_EXECUTOR.scheduleWithFixedDelay(this::reap, 10, 10, TimeUnit.MILLISECONDS);
  }

  public void register(TrackedOngoingRequest request) {
    outstanding.add(request);
  }

  public boolean remove(TrackedOngoingRequest request) {
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
    final Set<TrackedOngoingRequest> requests = ImmutableSet.copyOf(outstanding);
    for (TrackedOngoingRequest id : requests) {
      final boolean removed = outstanding.remove(id);
      if (removed) {
        id.reply(Response.forStatus(Status.SERVICE_UNAVAILABLE));
      }
    }
  }
}
