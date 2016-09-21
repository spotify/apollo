/*
 * -\-\-
 * Spotify Apollo HTTP Service
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
package com.spotify.apollo.httpservice;

import com.spotify.apollo.AppInit;
import com.spotify.apollo.Environment;
import com.spotify.apollo.RequestMetadata;
import com.spotify.apollo.Response;
import com.spotify.apollo.core.Service;
import com.spotify.apollo.route.AsyncHandler;
import com.spotify.apollo.route.Route;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import okio.ByteString;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class HttpServiceTest {

  // we need a static counter in order to be able to use the Class overload of
  // HttpService.boot
  private static final AtomicInteger counter = new AtomicInteger();

  @Rule
  public ExpectedException exception = ExpectedException.none();

  private final ExecutorService executorService = Executors.newSingleThreadExecutor();

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
        e.printStackTrace();
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

  @Test
  public void shouldPopulateRequestMetadata() throws Exception {
    AtomicReference<RequestMetadata> metadataReference = new AtomicReference<>();

    final InstanceWaiter waiter = new InstanceWaiter();

    Future<?> future = executorService.submit(() -> {
      try {
        HttpService.boot(environment ->
                             environment.routingEngine()
                                 .registerRoute(Route.async("GET", "/meta", trackMeta(metadataReference))),
                         "metadatatest",
                         waiter);
      } catch (LoadingException e) {
        throw new RuntimeException(e);
      }
    });

    final Service.Instance instance = waiter.waitForInstance();

    Request httpRequest = new Request.Builder()
        .url("http://localhost:29432/meta")
        .build();

    new OkHttpClient().newCall(httpRequest).execute();

    assertThat(metadataReference.get().protocol(), is("HTTP/1.1"));

    instance.getSignaller().signalShutdown();
    instance.waitForShutdown();
    future.get();
  }

  private AsyncHandler<Response<ByteString>> trackMeta(AtomicReference<RequestMetadata> metadata) {
    return requestContext -> {
      System.out.println("meta: " + requestContext.metadata());
      metadata.set(requestContext.metadata());
      return CompletableFuture.completedFuture(Response.<ByteString>ok());
    };
  }

  private static class BrokenService implements AppInit {

    @Override
    public void create(Environment environment) {
      throw new RuntimeException("I'm bork");
    }
  }

  private static class InstanceWaiter implements InstanceListener {

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
