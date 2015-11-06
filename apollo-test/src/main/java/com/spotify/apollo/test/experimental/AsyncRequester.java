/*
 * Copyright (c) 2014 Spotify AB
 */

package com.spotify.apollo.test.experimental;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.System.nanoTime;

public class AsyncRequester {

  private static final Logger LOG = LoggerFactory.getLogger(AsyncRequester.class);

  protected final ScheduledExecutorService executor;

  public AsyncRequester(ScheduledExecutorService executor) {
    this.executor = executor;
  }

  public <T> ListenableFuture<Void> pump(final int rps, final int runtimeSeconds,
                                         final ResponseTimeMetric metric,
                                         final Callable<ListenableFuture<T>> request) {

    final AtomicInteger rate = new AtomicInteger();
    final int hundredth = rps / 100;

    final Runnable command = new Runnable() {
      @Override
      public void run() {
        try {
          for (int i = 0; i < 100; i++) {
            if (metric.activeTracked.get() > 10000) {
              metric.totalRejected.incrementAndGet();
              continue;
            }

            final long t0 = nanoTime();
            final ListenableFuture<T> future = request.call();
            metric.track(future, t0, rate.get());
          }
        } catch (Exception e) {
          LOG.error("exception", e);
        }
      }
    };

    class PhaseSwitcher implements Runnable {

      private final ScheduledFuture<?> prev;
      final int factor;

      PhaseSwitcher(ScheduledFuture<?> prev, int factor) {
        this.prev = prev;
        this.factor = factor;
      }

      PhaseSwitcher(int factor) {
        this(null, factor);
      }

      @Override
      public void run() {
        if (prev != null) {
          prev.cancel(false);
        }

        if (factor == 100) {
          LOG.info("running @ 100% = " + rps + " rps");
        } else {
          LOG.info("running @ " + factor + "% = " + rps * factor / 100 + " rps");
        }
        rate.set(factor * hundredth);
        final ScheduledFuture<?> phaseSchedule =
            executor.scheduleAtFixedRate(command, 0, 1000000 / (hundredth * factor / 100),
                                         TimeUnit.MICROSECONDS);

        if (factor < 100) {
          final int step = factor < 80 ? 10 : 5;
          executor.schedule(new PhaseSwitcher(phaseSchedule, factor + step),
                            20, TimeUnit.SECONDS);
        }
      }
    }

    executor.execute(new PhaseSwitcher(10));

    final SettableFuture<Void> finish = SettableFuture.create();

    executor.schedule(new Runnable() {
      @Override
      public void run() {
        finish.set(null);
      }
    }, runtimeSeconds, TimeUnit.SECONDS);

    return finish;
  }

}
