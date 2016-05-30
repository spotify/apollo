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

import com.google.common.base.Stopwatch;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertThat;

public class ExecutorServiceCloserTest {

  private ExecutorServiceCloser closer;

  private ExecutorService service ;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Before
  public void setUp() throws Exception {
    service = Executors.newSingleThreadExecutor();

    closer = ExecutorServiceCloser.of(service);
  }

  @Test
  public void shouldShutdownTheExecutorOnClose() throws Exception {
    closer.close();

    assertThat(service.isShutdown(), is(true));
  }

  @Test
  public void shouldBeSafeToInvokeMultipleTimes() throws Exception {
    closer.close();
    closer.close();

    assertThat(service.isShutdown(), is(true));
  }

  @Test(timeout = 1000)
  public void shouldNotWaitIfNotConfiguredTo() throws Exception {
    service.execute(() -> {
      try {
        Thread.sleep(2000L);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    });

    closer.close();
  }

  @Test
  public void shouldWaitIfConfiguredTo() throws Exception {
    service.execute(() -> {
      try {
        Thread.sleep(2000L);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    });

    Stopwatch stopwatch = Stopwatch.createStarted();
    closer.withTimeout(Duration.ofMillis(3000L)).close();

    assertThat(stopwatch.elapsed(TimeUnit.MILLISECONDS), greaterThan(1000L));
  }

  @Test
  public void shouldThrowExceptionIfConfiguredWaitTimesOut() throws Exception {
    service.execute(() -> {
      try {
        Thread.sleep(2000L);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    });

    thrown.expect(ExecutorServiceCloser.WaitTimedOutException.class);
    closer.withTimeout(Duration.ofMillis(500L)).close();
  }

  @Test
  public void shouldNotBeInterruptibleWhenAwaitingTermination() throws Exception {
    service.execute(() -> {
      try {
        Thread.sleep(1000L);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    });

    Thread t = new Thread(() -> {
      try {
        closer.withTimeout(Duration.ofMillis(2000)).close();
      } catch (ExecutorServiceCloser.WaitTimedOutException e) {
        throw new RuntimeException(e);
      }
    });

    t.start();

    // give the thread some time to start executing the closer
    Thread.sleep(100);
    t.interrupt();

    Stopwatch stopwatch = Stopwatch.createStarted();
    t.join();

    // validate that we waited something like the full 1000 ms required for the task to finish
    assertThat(stopwatch.elapsed(TimeUnit.MILLISECONDS), greaterThan(500L));
  }
}