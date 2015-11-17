/*
 * -\-\-
 * Spotify Apollo Testing Helpers
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
package com.spotify.apollo.test.experimental;

import com.google.common.base.Function;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.util.concurrent.Futures.addCallback;
import static java.lang.System.nanoTime;

public class ResponseTimeMetric {

  private static final Logger LOG = LoggerFactory.getLogger(ResponseTimeMetric.class);

  ResponseTimeMetric() {
  }

  static final int BUFFER_SIZE = 8 * 1024;

  public final AtomicInteger totalResponses = new AtomicInteger();
  public final AtomicInteger totalFailed = new AtomicInteger();
  public final AtomicInteger totalRejected = new AtomicInteger();
  public final AtomicInteger activeTracked = new AtomicInteger();

  final SynchronizedRingBuffer<Long> responseTimes = new SynchronizedRingBuffer<>(BUFFER_SIZE);
  final Long[] longs = new Long[BUFFER_SIZE];

  Function<CallResult, Void> callback;

  public void setCallback(Function<CallResult, Void> callback) {
    this.callback = callback;
  }

  public long getAverageTime() {
    responseTimes.copy(longs);
    long total = 0;
    for (Long responseTime : longs) {
      if (responseTime != null) {
        total += responseTime;
      }
    }
    return total / BUFFER_SIZE;
  }

  public <T> void track(final ListenableFuture<T> future, final long t0, final int rate) {
    addCallback(future, createCallback(t0, rate));
  }

  private <T> FutureCallback<T> createCallback(final long t0, final int rate) {
    activeTracked.incrementAndGet();
    return new FutureCallback<T>() {
      @Override
      public void onSuccess(T result) {
        long elapsed = nanoTime() - t0;

        responseTimes.offer(elapsed);
        totalResponses.incrementAndGet();
        activeTracked.decrementAndGet();

        if (callback != null) {
          callback.apply(new CallResult(elapsed, true, rate));
        }
      }

      @Override
      public void onFailure(Throwable t) {
        LOG.error("Failed execution", t);
        totalResponses.incrementAndGet();
        totalFailed.incrementAndGet();
        activeTracked.decrementAndGet();
      }
    };
  }

  public static class CallResult {
    public final long elapsed;
    public final boolean success;
    public final int rate;

    public CallResult(long elapsed, boolean success, int rate) {
      this.elapsed = elapsed;
      this.success = success;
      this.rate = rate;
    }
  }
}
