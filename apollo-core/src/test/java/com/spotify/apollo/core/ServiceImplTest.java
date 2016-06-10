/*
 * -\-\-
 * Spotify Apollo Service Core (aka Leto)
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
package com.spotify.apollo.core;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.math.IntMath;
import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.Provides;

import com.spotify.apollo.module.AbstractApolloModule;
import com.spotify.apollo.module.ApolloModule;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Named;

import static com.spotify.apollo.core.Services.INJECT_ARGS;
import static com.spotify.apollo.core.Services.INJECT_ENVIRONMENT;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ServiceImplTest {

  private ScheduledExecutorService executorService;

  @Before
  public void setUp() throws Exception {
    executorService = Executors.newSingleThreadScheduledExecutor();
  }

  @Test
  public void testEmpty() throws Exception {
    Service service = ServiceImpl.builder("test").build();

    assertThat(service.getServiceName(), is("test"));

    try (Service.Instance instance = service.start()) {
      assertThat(instance.getService(), is(service));
      assertThat(instance.getUnprocessedArgs(), is(empty()));
    }
  }

  @Test
  public void shouldRetainArgs() throws Exception {
    Service service = ServiceImpl.builder("test").build();

    try (Service.Instance instance = service.start("-Dconfig=value")) {
      assertThat(instance.getUnprocessedArgs(), is(ImmutableList.of("-Dconfig=value")));
    }
  }

  @Test
  public void shouldInjectArgs() throws Exception {
    Service service = ServiceImpl.builder("test")
        .withModule(new ArgsAndEnvCapturingModule())
        .build();

    try (Service.Instance instance = service.start("arg1", "arg2", "argC")) {
      assertThat(instance.resolve(Args.class).args, equalTo(ImmutableList.of("arg1", "arg2", "argC")));
    }
  }

  @Test
  public void shouldInjectEnvironment() throws Exception {
    Service service = ServiceImpl.builder("test")
        .withModule(new ArgsAndEnvCapturingModule())
        .build();

    Map<String, String> envMap = ImmutableMap.of("hi", "ht", "asdas", "ddsf");

    try (Service.Instance instance = service.start(new String[] { "-Dconfig=value" }, envMap)) {
      assertThat(instance.resolve(Env.class).env, equalTo(envMap));
    }
  }

  @Test(timeout = 1000)
  public void testSignalShutdown() throws Exception {
    Service service = ServiceImpl.builder("test").build();
    try (final Service.Instance instance = service.start("--syslog=false")) {

      instance.getSignaller().signalShutdown();

      instance.waitForShutdown();
    }
  }

  @Test(timeout = 1000, expected = InterruptedException.class)
  public void testInterrupt() throws Exception {
    Service service = ServiceImpl.builder("test").withShutdownInterrupt(true).build();

    try (Service.Instance instance = service.start()) {
      executorService
          .schedule(new Shutdowner(instance.getSignaller()), 5, TimeUnit.MILLISECONDS);
      new CountDownLatch(1).await(); // Wait forever
    }
  }

//  @Test
//  public void testHelpFallthrough() throws Exception {
//    Service service = ServiceImpl.builder("test").withCliHelp(false).build();
//
//    try (Service.Instance instance = service.start("--help", "-h")) {
//      assertThat(instance.getUnprocessedArgs(), contains("--help", "-h"));
//    }
//  }

  @Test
  public void testMultipleModules() throws Exception {
    AtomicInteger count = new AtomicInteger(0);
    Service service = ServiceImpl.builder("test")
        .withModule(new CountingModuleWithPriority(1.0, count))
        .withModule(new CountingModuleWithPriority(1.0, count))
        .build();

    try (Service.Instance instance = service.start()) {
      instance.getSignaller().signalShutdown();
      instance.waitForShutdown();
    }

    assertThat(count.get(), is(2));
  }

  @Test
  public void testModuleClassesAreLifecycleManaged() throws Exception {
    AtomicBoolean created = new AtomicBoolean(false);
    AtomicBoolean closed = new AtomicBoolean(false);
    ModuleWithLifecycledKeys lifecycleModule = new ModuleWithLifecycledKeys(created, closed);
    Service service = ServiceImpl.builder("test")
        .withModule(lifecycleModule)
        .build();

    try (Service.Instance instance = service.start()) {
      instance.getSignaller().signalShutdown();
      instance.waitForShutdown();
    }

    assertThat(created.get(), is(true));
    assertThat(closed.get(), is(true));
  }

  @Test
  public void testResolve() throws Exception {
    AtomicBoolean created = new AtomicBoolean(false);
    AtomicBoolean closed = new AtomicBoolean(false);
    ModuleWithLifecycledKeys lifecycleModule = new ModuleWithLifecycledKeys(created, closed);
    Service service = ServiceImpl.builder("test")
        .withModule(lifecycleModule)
        .build();

    try (Service.Instance instance = service.start()) {
      ModuleWithLifecycledKeys.Foo foo =
          instance.resolve(ModuleWithLifecycledKeys.Foo.class);
      assertNotNull(foo);

      instance.getSignaller().signalShutdown();
      instance.waitForShutdown();
    }
  }

  @Test(expected = ApolloConfigurationException.class)
  public void testResolveInvalid() throws Exception {
    final Class<?> unusedClass = IntMath.class;

    Service service = ServiceImpl.builder("test").build();
    try (Service.Instance instance = service.start()) {
      instance.resolve(unusedClass);
    }
  }

  @Test(timeout = 1000)
  public void testCleanShutdown() throws Exception {
    Runtime runtime = mock(Runtime.class);
    Service service = ServiceImpl.builder("test")
        .withRuntime(runtime)
        .build();

    //noinspection EmptyTryBlock
    try (Service.Instance ignored = service.start()) {
      // Do nothing
    }

    // Simulate JVM shutdown hook
    ArgumentCaptor<Thread> threadCaptor = ArgumentCaptor.forClass(Thread.class);

    verify(runtime).addShutdownHook(threadCaptor.capture());

    // Should not block for more than 1 sec (will be interrupted by test timeout if it blocks)
    threadCaptor.getValue().run();
  }

  @Test(timeout = 1000)
  public void testExceptionDuringInit() throws Exception {
    Runtime runtime = mock(Runtime.class);
    Service service = ServiceImpl.builder("test")
        .withModule(new AbstractApolloModule() {
          @Override
          protected void configure() {
            throw new RuntimeException("Fail foobar");
          }

          @Override
          public String getId() {
            return "fail";
          }
        })
        .withRuntime(runtime)
        .build();

    try (Service.Instance instance = service.start()) {
      // Should not be reached
      fail("Service configuration should have failed due to 'fail' module but didn't.");
    } catch (Throwable ex) {
      // Should be due to the 'fail' module above
      assertTrue(ex instanceof RuntimeException);
      assertThat(ex.getMessage(), containsString("Fail foobar"));
    }

    // Simulate JVM shutdown hook
    ArgumentCaptor<Thread> threadCaptor = ArgumentCaptor.forClass(Thread.class);

    verify(runtime).addShutdownHook(threadCaptor.capture());

    // Should not block for more than 1 sec (will be interrupted by test timeout if it blocks)
    threadCaptor.getValue().run();
  }

  @Test(timeout = 1000, expected = InterruptedException.class)
  public void testSimulateCtrlCInterrupted() throws Exception {
    final Runtime runtime = mock(Runtime.class);
    Service service = ServiceImpl.builder("test")
        .withRuntime(runtime)
        .withShutdownInterrupt(true)
        .build();
    ExecutorService executorService = Executors.newSingleThreadExecutor();

    try (Service.Instance instance = service.start()) {
      executorService.submit(new ShutdownHookSim(runtime));
      new CountDownLatch(1).await();
    } finally {
      executorService.shutdownNow();
    }
  }

  @Test(timeout = 1000)
  public void testSimulateCtrlCWaitingForShutdown() throws Exception {
    final Runtime runtime = mock(Runtime.class);
    Service service = ServiceImpl.builder("test")
        .withRuntime(runtime)
        .build();

    ExecutorService executorService = Executors.newSingleThreadExecutor();

    try (Service.Instance instance = service.start()) {
      executorService.submit(new ShutdownHookSim(runtime));
      instance.waitForShutdown();
    } catch (Throwable ex) {
      fail("Not clean shutdown");
    } finally {
      executorService.shutdownNow();
    }
  }

  private static class Shutdowner implements Callable<Void> {

    private final Service.Signaller signaller;

    Shutdowner(Service.Signaller signaller) {
      this.signaller = signaller;
    }

    @Override
    public Void call() throws Exception {
      signaller.signalShutdown();
      return null;
    }
  }

  private static class ShutdownHookSim implements Callable<Void> {

    private final Runtime runtime;

    ShutdownHookSim(Runtime runtime) {
      this.runtime = runtime;
    }

    @Override
    public Void call() throws Exception {
      // Simulate JVM shutdown hook
      ArgumentCaptor<Thread> threadCaptor = ArgumentCaptor.forClass(Thread.class);

      verify(runtime).addShutdownHook(threadCaptor.capture());

      // This is what simulates the shutdown hook
      threadCaptor.getValue().run();
      return null;
    }
  }

  private static class ArgsAndEnvCapturingModule implements ApolloModule {

    @Override
    public String getId() {
      return "argscaptor";
    }

    @Override
    public double getPriority() {
      return 0;
    }

    @Override
    public Set<? extends Key<?>> getLifecycleManaged() {
      return Collections.emptySet();
    }

    @Override
    public void configure(Binder binder) {

    }

    @Provides
    public Args args(@Named(INJECT_ARGS) ImmutableList<String> args) {
      return new Args(args);
    }

    @Provides
    public Env env(@Named(INJECT_ENVIRONMENT) Map<String, String> env) {
      return new Env(env);
    }
  }

  private static class Args {
    private final List<String> args;

    private Args(List<String> args) {
      this.args = args;
    }
  }

  private static class Env {
    private final Map<String, String> env;

    private Env(Map<String, String> env) {
      this.env = env;
    }
  }
}
