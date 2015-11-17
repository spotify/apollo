/*
 * Copyright (c) 2013-2015 Spotify AB
 */

package com.spotify.apollo.httpservice;

import com.spotify.apollo.AppInit;
import com.spotify.apollo.Environment;
import com.spotify.apollo.core.Service;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class HttpServiceTest {

  // we need a static counter in order to be able to use the Class overload of
  // StandaloneService.boot
  static final AtomicInteger counter = new AtomicInteger();

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Before
  public void setUp() throws Exception {
    counter.set(0);
  }

  @Test
  public void testCreateWithAppInit() throws Exception {
    final InstanceWaiter waiter = new InstanceWaiter();
    final CountDownLatch closed = new CountDownLatch(1);
    final AppInit appInit = env -> {
      counter.incrementAndGet();
      env.closer().register(closed::countDown);
    };

    new Thread(() -> {
      try {
        HttpService.boot(appInit, "test", waiter, "run", "foo");
      } catch (LoadingException e) {
        fail(e.getMessage());
      }
    }).start();

    final Service.Instance instance = waiter.waitForInstance();
    instance.getSignaller().signalShutdown();
    instance.waitForShutdown();

    closed.await(5000, TimeUnit.SECONDS);
    assertEquals(1, counter.get());
  }

  @Test
  public void testBrokenServiceSimple() throws Exception {
    exception.expect(LoadingException.class);
    HttpService.boot(new BrokenService(), "test", "run", "foo");
  }

  @Test
  public void testBrokenService() throws Exception {
    final Service service = HttpService.usingAppInit(new BrokenService(), "test").build();

    exception.expect(LoadingException.class);
    HttpService.boot(service, "run", "foo");
  }

  static class BrokenService implements AppInit {

    @Override
    public void create(Environment environment) {
      throw new RuntimeException("I'm bork");
    }
  }

  static class InstanceWaiter implements InstanceListener {

    private final CountDownLatch latch = new CountDownLatch(1);

    private volatile Service.Instance instance;

    @Override
    public void instanceCreated(Service.Instance instance) {
      assertNull(this.instance);
      this.instance = instance;
      latch.countDown();
    }

    Service.Instance waitForInstance() throws InterruptedException {
      latch.await();
      assertNotNull(instance);
      return instance;
    }
  }
}
