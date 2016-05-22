/*
 * -\-\-
 * Spotify Apollo Extra
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
package com.spotify.apollo.concurrent;

import com.spotify.apollo.Environment;

import java.io.Closeable;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

/**
 * A utility class to make it easier to get the {@link com.google.common.io.Closer} returned by
 * {@link Environment#closer()} to handle shutting down {@link ExecutorService} instances.
 */
// optional is useful as a field and parameter type in this class
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class ExecutorServiceCloser implements Closeable {

  private final ExecutorService executorService;
  private final Optional<Duration> timeout;

  private ExecutorServiceCloser(ExecutorService executorService, Optional<Duration> timeout) {
    this.executorService = requireNonNull(executorService);
    this.timeout = requireNonNull(timeout);

    timeout
        .ifPresent(duration -> checkArgument(!duration.isNegative(), "Timeout must be positive"));
  }

  /**
   * Returns a new ExecutorServiceCloser for the same executor service as this, but with
   * a timeout specied by the {@code timeout} parameter.
   */
  public ExecutorServiceCloser withTimeout(Duration timeout) {
    return new ExecutorServiceCloser(executorService, Optional.of(timeout));
  }

  /**
   * Create an ExecutorServiceCloser for the supplied ExecutorService. This closer will not have
   * an associated timeout.
   */
  public static ExecutorServiceCloser of(ExecutorService executorService) {
    return new ExecutorServiceCloser(executorService, Optional.empty());
  }

  /**
   * Shuts down the associated ExecutorService using the {@link ExecutorService#shutdown()}
   * method. If the closer has a configured timeout, it will wait for the ExecutorService to
   * complete executing its current tasks, or until the timeout expires. If the timeout expires,
   * a {@link WaitTimedOutException} will be thrown.
   *
   * This method is idempotent; repeated invocations have no effect.
   *
   * @throws WaitTimedOutException if a timeout is configured and the executor service doesn't
   *                               finish shutting down before the timeout expires.
   */
  @Override
  public void close() throws WaitTimedOutException {
    executorService.shutdown();

    timeout.ifPresent(
        timeout -> {
          if (!awaitTerminationUninterruptibly(timeout)) {
            throw new WaitTimedOutException(timeout);
          }
        }
    );
  }

  private boolean awaitTerminationUninterruptibly(Duration timeout) {
    // this implementation stolen with pride from Guava's Uninterruptibles class.
    boolean interrupted = false;
    try {
      long remainingNanos = timeout.toNanos();
      long end = System.nanoTime() + remainingNanos;

      // the difference compared to the Uninterruptibles implementations is this condition:
      // since there are many ExecutorService implementations, I don't want to rely on how they
      // deal with negative timeouts in the awaitTermination method.
      while (remainingNanos > 0) {
        try {
          return executorService.awaitTermination(remainingNanos, NANOSECONDS);
        } catch (InterruptedException e) {
          interrupted = true;
          remainingNanos = end - System.nanoTime();
        }
      }

      return false;
    } finally {
      if (interrupted) {
        Thread.currentThread().interrupt();
      }
    }
  }

  /**
   * Thrown if a configured wait timeout has expired before the executor service was shut down.
   */
  public static class WaitTimedOutException extends RuntimeException {

    public WaitTimedOutException(Duration timeout) {
      super("wait timed out after " + timeout);
    }
  }
}
