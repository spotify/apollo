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


import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import com.spotify.apollo.test.StubClient;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;

import static com.google.common.util.concurrent.Futures.immediateFuture;
import static java.lang.Integer.toHexString;
import static java.util.concurrent.Executors.newScheduledThreadPool;

public final class PerformanceFixture {

  private static final int DEFAULT_THREADS = Runtime.getRuntime().availableProcessors() * 2;

  public final ScheduledExecutorService executor;
  public final StubClient client;

  public PerformanceFixture() {
    this(DEFAULT_THREADS);
  }

  public PerformanceFixture(final int threads) {
    final String className = getClass().getSimpleName();
    final String name = className + '-' + toHexString(hashCode());

    final ThreadFactoryBuilder threadFactoryBuilder = new ThreadFactoryBuilder()
        .setNameFormat("loadtest-" + name + "-%d")
        .setUncaughtExceptionHandler(new ThreadExceptionHandler());
    executor = newScheduledThreadPool(threads, threadFactoryBuilder.build());

    client = new StubClient(executor);
  }

  /**
   * Create a metric for tracking load tests.
   */
  public ResponseTimeMetric createMetric() {
    return new ResponseTimeMetric();
  }

  /**
   * Run a load test with the given parameters.
   *
   * @param rps     Request per second when at full rate.
   * @param runtime Number of seconds to run.
   * @param metric  A ResponseTimeMetric to use for tracking requests.
   * @return A Future that will be finished when 'runtime' seconds has passed.
   */
  public ListenableFuture<Void> pump(int rps, int runtime, ResponseTimeMetric metric) {
    final AsyncRequester asyncRequester = new AsyncRequester(executor);

    return asyncRequester.pump(rps, runtime, metric, new Callable<ListenableFuture<Object>>() {
      @Override
      public ListenableFuture<Object> call() throws Exception {
        return PerformanceFixture.this.call();
      }
    });
  }

  /**
   * Dummy implementation that should be overriden if doing perf tests with pump.
   */
  protected <T> ListenableFuture<T> call() {
    return immediateFuture((T) null);
  }

  public void clear()  {
    executor.shutdown();
  }
}
