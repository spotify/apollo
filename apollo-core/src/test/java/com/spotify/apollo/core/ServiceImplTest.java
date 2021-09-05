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

import static com.spotify.apollo.core.Services.CommonConfigKeys.APOLLO_ARGS_CORE;
import static com.spotify.apollo.core.Services.CommonConfigKeys.APOLLO_ARGS_UNPARSED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.math.IntMath;
import com.spotify.apollo.module.AbstractApolloModule;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

public class ServiceImplTest {

  static List<Map<String, String>> env() {
    //TODO: Use regular JDk methods instead of guava if/when we remove JDK8 support
    return ImmutableList.of(
        ImmutableMap.of("APOLLO_SPRING_ENABLED", "true"),
        ImmutableMap.of("APOLLO_SPRING_ENABLED", "false"),
        ImmutableMap.of()
    );
  }

  private static <K, V> Map<K, V> append(Map<K, V> first, Map<K, V> second) {
    return ImmutableMap.<K, V>builder().putAll(first).putAll(second).build();
  }

  private static Config envAsConfig(Map<String, String> env) {
    return ConfigFactory.parseMap(
        env.entrySet().stream().collect(Collectors.toMap(
            entry -> entry.getKey().toLowerCase().replaceAll("_", ".")
            , Map.Entry::getValue)));
  }

  @ParameterizedTest
  @MethodSource("env")
  public void testEmpty(Map<String, String> env) throws Exception {
    Service service = ServiceImpl.builder("test").build();

    assertThat(service.getServiceName(), is("test"));

    try (Service.Instance instance = service.start(new String[]{}, env)) {
      assertThat(instance.getService(), is(service));
      assertThat(instance.getUnprocessedArgs(), is(empty()));
    }
  }

  @ParameterizedTest
  @MethodSource("env")
  public void testConfig(Map<String, String> env) throws Exception {
    Service service = ServiceImpl.builder("test").build();

    try (Service.Instance instance = service.start(new String[]{"-Dconfig=value"}, env)) {
      assertThat(instance.getUnprocessedArgs(), is(empty()));
      assertThat(instance.getConfig().getString("config"), is("value"));
    }
  }

  @ParameterizedTest
  @MethodSource("env")
  public void testEnvConfig(Map<String, String> env) throws Exception {
    Service service = ServiceImpl.builder("test").build();

    try (Service.Instance instance = service
        .start(new String[]{}, append(env, ImmutableMap.of("APOLLO_A_B", "value")))) {
      assertThat(instance.getConfig().getString("a.b"), is("value"));
    }
  }

  @ParameterizedTest
  @MethodSource("env")
  public void testEnvConfigCustomPrefix(Map<String, String> env) throws Exception {
    Service service = ServiceImpl.builder("test").withEnvVarPrefix("horse").build();

    try (Service.Instance instance = service
        .start(new String[]{}, append(env, ImmutableMap.of("horse_A_B", "value")))) {
      assertThat(instance.getConfig().getString("a.b"), is("value"));
    }
  }

  @ParameterizedTest
  @MethodSource("env")
  public void testEnvConfigWithLeadingUnderscores(Map<String, String> env) throws Exception {
    Service service = ServiceImpl.builder("test").build();

    try (Service.Instance instance = service
        .start(new String[]{}, append(env, ImmutableMap.of("APOLLO___A_B", "value")))) {
      assertThat(instance.getConfig().getString("_a.b"), is("value"));
    }
  }

  @ParameterizedTest
  @MethodSource("env")
  public void testEnvConfigWithUnderscoresEverywhere(Map<String, String> env) throws Exception {
    Service service = ServiceImpl.builder("test").build();

    try (Service.Instance instance = service
        .start(new String[]{},
               append(env, ImmutableMap.of("APOLLO_A___B__C______D_____E__", "value")))) {
      assertThat(instance.getConfig().getString("a._b_c___d.__e_"), is("value"));
    }
  }

  @ParameterizedTest
  @MethodSource("env")
  public void testOverlaysExplicitConfigFile(Map<String, String> env) throws IOException {
    Service service = ServiceImpl.builder("test").build();

    try (Service.Instance instance = service.start(
        new String[]{"--config", "src/test/files/overlay.conf"}, env)) {
      Config config = instance.getConfig();
      assertThat(config.getString("bundled.value"), is("is loaded"));
      assertThat(config.getString("bundled.shadowed"), is("overlayed"));
      assertThat(config.getString("some.key"), is("has a value"));
    }
  }

  @ParameterizedTest
  @MethodSource("env")
  public void testUsesConfig(Map<String, String> env) throws IOException {
    Service service = ServiceImpl.builder("test").build();

    Config config = ConfigFactory.empty()
        .withValue("this.key", ConfigValueFactory.fromAnyRef("value for this.key"));

    try (Service.Instance instance = service.start(new String[]{}, config)) {
      assertThat(instance.getConfig().getString("this.key"), is("value for this.key"));
    }
  }

  @ParameterizedTest
  @MethodSource("env")
  public void testConfigSupportsColonValues(Map<String, String> env) throws Exception {
    Service service = ServiceImpl.builder("test").build();

    try (Service.Instance instance = service.start(new String[]{"-Dconfig=value:more"}, env)) {
      assertThat(instance.getUnprocessedArgs(), is(empty()));
      assertThat(instance.getConfig().getString("config"), is("value:more"));
    }
  }

  @ParameterizedTest
  @MethodSource("env")
  public void testConfigSupportsEqualsValues(Map<String, String> env) throws Exception {
    Service service = ServiceImpl.builder("test").build();

    try (Service.Instance instance = service.start(new String[]{"-Dconfig=value=more"}, env)) {
      assertThat(instance.getUnprocessedArgs(), is(empty()));
      assertThat(instance.getConfig().getString("config"), is("value=more"));
    }
  }

  @ParameterizedTest
  @MethodSource("env")
  public void testUnresolved(Map<String, String> env) throws Exception {
    Service service = ServiceImpl.builder("test").build();

    try (Service.Instance instance = service.start(new String[]{"-Dfoo=bar", "hello"}, env)) {
      assertThat(instance.getUnprocessedArgs(), contains("hello"));
      assertThat(instance.getConfig().getString("foo"), is("bar"));
    }
  }

  @ParameterizedTest
  @MethodSource("env")
  public void testArgsDone(Map<String, String> env) throws Exception {
    Service service = ServiceImpl.builder("test").build();

    try (Service.Instance instance = service.start(
        new String[]{"-Dfoo=bar", "--", "xyz", "-Dbar=baz", "abc"}, env)) {
      assertThat(instance.getUnprocessedArgs(), contains("xyz", "-Dbar=baz", "abc"));
      assertThat(instance.getConfig().getString("foo"), is("bar"));
      assertThat(instance.getConfig().hasPath("bar"), is(false));
    }
  }

  @ParameterizedTest
  @MethodSource("env")
  public void testArgsAreInConfig(Map<String, String> env) throws Exception {
    Service service = ServiceImpl.builder("test").build();

    try (Service.Instance instance = service.start(
        new String[]{"-Dfoo=bar", "--", "xyz", "-Dbar=baz", "abc"}, env)) {
      assertThat(instance.getConfig().hasPath(APOLLO_ARGS_CORE.getKey()), is(true));
      assertThat(instance.getConfig().hasPath(APOLLO_ARGS_UNPARSED.getKey()), is(true));
      assertThat(instance.getConfig().getStringList(APOLLO_ARGS_CORE.getKey()),
                 contains("-Dfoo=bar", "--", "xyz", "-Dbar=baz", "abc"));
      assertThat(instance.getConfig().getStringList(APOLLO_ARGS_UNPARSED.getKey()),
                 contains("xyz", "-Dbar=baz", "abc"));
    }
  }

  @ParameterizedTest
  @MethodSource("env")
  public void testVerbosity(Map<String, String> env) throws Exception {
    Service service = ServiceImpl.builder("test").build();

    try (Service.Instance instance = service.start(new String[]{"-v"}, env)) {
      assertThat(instance.getUnprocessedArgs(), is(empty()));
      assertThat(instance.getConfig().getInt("logging.verbosity"), is(1));
    }
  }

  @ParameterizedTest
  @MethodSource("env")
  public void testVerbosityLong(Map<String, String> env) throws Exception {
    Service service = ServiceImpl.builder("test").build();

    try (Service.Instance instance = service.start(new String[]{"--verbose"}, env)) {
      assertThat(instance.getUnprocessedArgs(), is(empty()));
      assertThat(instance.getConfig().getInt("logging.verbosity"), is(1));
    }
  }

  @ParameterizedTest
  @MethodSource("env")
  public void testVerbosityLongMany() throws Exception {
    Service service = ServiceImpl.builder("test").build();

    try (Service.Instance instance = service.start("--verbose", "--verbose")) {
      assertThat(instance.getUnprocessedArgs(), is(empty()));
      assertThat(instance.getConfig().getInt("logging.verbosity"), is(2));
    }
  }

  @ParameterizedTest
  @MethodSource("env")
  public void testVerbosityStacked() throws Exception {
    Service service = ServiceImpl.builder("test").build();

    try (Service.Instance instance = service.start("-vvvv")) {
      assertThat(instance.getUnprocessedArgs(), is(empty()));
      assertThat(instance.getConfig().getInt("logging.verbosity"), is(4));
    }
  }

  @ParameterizedTest
  @MethodSource("env")
  public void testVerbosityMany() throws Exception {
    Service service = ServiceImpl.builder("test").build();

    try (Service.Instance instance = service.start("-vv", "-vv")) {
      assertThat(instance.getUnprocessedArgs(), is(empty()));
      assertThat(instance.getConfig().getInt("logging.verbosity"), is(4));
    }
  }

  @ParameterizedTest
  @MethodSource("env")
  public void testVerbosityQuiet() throws Exception {
    Service service = ServiceImpl.builder("test").build();

    try (Service.Instance instance = service.start("-q")) {
      assertThat(instance.getUnprocessedArgs(), is(empty()));
      assertThat(instance.getConfig().getInt("logging.verbosity"), is(-3));
    }
  }

  @ParameterizedTest
  @MethodSource("env")
  public void testVerbosityQuietLong() throws Exception {
    Service service = ServiceImpl.builder("test").build();

    try (Service.Instance instance = service.start("--quiet")) {
      assertThat(instance.getUnprocessedArgs(), is(empty()));
      assertThat(instance.getConfig().getInt("logging.verbosity"), is(-3));
    }
  }

  @ParameterizedTest
  @MethodSource("env")
  public void testVerbosityQuietThenVerbose() throws Exception {
    Service service = ServiceImpl.builder("test").build();

    try (Service.Instance instance = service.start("--quiet", "--verbose")) {
      assertThat(instance.getUnprocessedArgs(), is(empty()));
      assertThat(instance.getConfig().getInt("logging.verbosity"), is(-2));
    }
  }

  @ParameterizedTest
  @MethodSource("env")
  public void testVerbosityConcise() throws Exception {
    Service service = ServiceImpl.builder("test").build();

    try (Service.Instance instance = service.start("-c")) {
      assertThat(instance.getUnprocessedArgs(), is(empty()));
      assertThat(instance.getConfig().getInt("logging.verbosity"), is(-1));
    }
  }

  @ParameterizedTest
  @MethodSource("env")
  public void testVerbosityConciseLong() throws Exception {
    Service service = ServiceImpl.builder("test").build();

    try (Service.Instance instance = service.start("--concise")) {
      assertThat(instance.getUnprocessedArgs(), is(empty()));
      assertThat(instance.getConfig().getInt("logging.verbosity"), is(-1));
    }
  }

  @ParameterizedTest
  @MethodSource("env")
  public void testVerbosityConciseLongMany() throws Exception {
    Service service = ServiceImpl.builder("test").build();

    try (Service.Instance instance = service.start("--concise", "--concise")) {
      assertThat(instance.getUnprocessedArgs(), is(empty()));
      assertThat(instance.getConfig().getInt("logging.verbosity"), is(-2));
    }
  }

  @ParameterizedTest
  @MethodSource("env")
  public void testVerbosityConciseStacked() throws Exception {
    Service service = ServiceImpl.builder("test").build();

    try (Service.Instance instance = service.start("-cccc")) {
      assertThat(instance.getUnprocessedArgs(), is(empty()));
      assertThat(instance.getConfig().getInt("logging.verbosity"), is(-4));
    }
  }

  @ParameterizedTest
  @MethodSource("env")
  public void testVerbosityConciseMany() throws Exception {
    Service service = ServiceImpl.builder("test").build();

    try (Service.Instance instance = service.start("-cc", "-cc")) {
      assertThat(instance.getUnprocessedArgs(), is(empty()));
      assertThat(instance.getConfig().getInt("logging.verbosity"), is(-4));
    }
  }

  @ParameterizedTest
  @MethodSource("env")
  public void testSyslog() throws Exception {
    Service service = ServiceImpl.builder("test").build();

    try (Service.Instance instance = service.start("--syslog")) {
      assertThat(instance.getUnprocessedArgs(), is(empty()));
      assertThat(instance.getConfig().getBoolean("logging.syslog"), is(true));
    }
  }

  @ParameterizedTest
  @MethodSource("env")
  public void testSyslogTrue() throws Exception {
    Service service = ServiceImpl.builder("test").build();

    try (Service.Instance instance = service.start("--syslog=true")) {
      assertThat(instance.getUnprocessedArgs(), is(empty()));
      assertThat(instance.getConfig().getBoolean("logging.syslog"), is(true));
    }
  }

  @ParameterizedTest
  @MethodSource("env")
  public void testSyslogFalse() throws Exception {
    Service service = ServiceImpl.builder("test").build();

    try (Service.Instance instance = service.start("--syslog=false")) {
      assertThat(instance.getUnprocessedArgs(), is(empty()));
      assertThat(instance.getConfig().getBoolean("logging.syslog"), is(false));
    }
  }

  @Test(expected = ApolloHelpException.class)
  public void testHelp() throws Exception {
    Service service = ServiceImpl.builder("test").build();
    Services.run(service, "--help");
  }

  @Timeout(1000)
  @ParameterizedTest
  @MethodSource("env")
  public void testSignalShutdown() throws Exception {
    Service service = ServiceImpl.builder("test").build();
    try (final Service.Instance instance = service.start("--syslog=false")) {

      // Force predictable concurrency of signal and main loop start
      final CountDownLatch countDownLatch = new CountDownLatch(1);
      instance.getExecutorService().submit(() -> {
        countDownLatch.await();
        Thread.sleep(200);
        instance.getSignaller().signalShutdown();
        return null;
      });

      countDownLatch.countDown();
      instance.waitForShutdown();
    }
  }

  @ParameterizedTest
  @MethodSource("env")
  public void testUnresolvedMixed() throws Exception {
    // Issue #8
    Service service = ServiceImpl.builder("test").build();

    try (Service.Instance instance = service.start("--hello", "-Dfoo=bar", "bye")) {
      assertThat(instance.getUnprocessedArgs(), contains("--hello", "bye"));
      assertThat(instance.getConfig().getString("foo"), is("bar"));
    }
  }

  @Timeout(1000)
  @ParameterizedTest
  @MethodSource("env")
  public void testInterrupt(final Map<String, String> env) {
    assertThrows(InterruptedException.class, () -> {
      Service service = ServiceImpl.builder("test").withShutdownInterrupt(true).build();

      try (Service.Instance instance = service.start(new String[]{}, env)) {
        instance.getScheduledExecutorService()
            .schedule(new Shutdowner(instance.getSignaller()), 5, TimeUnit.MILLISECONDS);
        new CountDownLatch(1).await(); // Wait forever
      }
    });
  }

  @ParameterizedTest
  @MethodSource("env")
  public void testHelpFallthrough() throws Exception {
    Service service = ServiceImpl.builder("test").withCliHelp(false).build();

    try (Service.Instance instance = service.start("--help", "-h")) {
      assertThat(instance.getUnprocessedArgs(), contains("--help", "-h"));
    }
  }

  @ParameterizedTest
  @MethodSource("env")
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

  @ParameterizedTest
  @MethodSource("env")
  public void testModuleClassesAreLifecycleManaged(Map<String, String> env) throws Exception {
    AtomicBoolean created = new AtomicBoolean(false);
    AtomicBoolean closed = new AtomicBoolean(false);
    ModuleWithLifecycledKeys lifecycleModule = new ModuleWithLifecycledKeys(created, closed);
    Service service = ServiceImpl.builder("test")
        .withModule(lifecycleModule)
        .build();

    try (Service.Instance instance = service.start(new String[]{}, env)) {
      instance.getSignaller().signalShutdown();
      instance.waitForShutdown();
    }

    assertThat(created.get(), is(true));
    assertThat(closed.get(), is(true));
  }

  @ParameterizedTest
  @MethodSource("env")
  public void testChildModuleClassesAreLifecycleManaged(Map<String, String> env) throws Exception {
    AtomicBoolean created = new AtomicBoolean(false);
    AtomicBoolean closed = new AtomicBoolean(false);
    AtomicBoolean childCreated = new AtomicBoolean(false);
    AtomicBoolean childClosed = new AtomicBoolean(false);
    ModuleWithLifecycledKeys lifecycleModule =
        new ModuleWithLifecycledKeys(created, closed, childCreated, childClosed);
    Service service = ServiceImpl.builder("test")
        .withModule(lifecycleModule)
        .build();

    try (Service.Instance instance = service.start(new String[]{}, env)) {
      instance.getSignaller().signalShutdown();
      instance.waitForShutdown();
    }

    assertThat(childCreated.get(), is(true));
    assertThat(childClosed.get(), is(true));
  }

  @ParameterizedTest
  @MethodSource("env")
  public void testResolve(Map<String, String> env) throws Exception {
    AtomicBoolean created = new AtomicBoolean(false);
    AtomicBoolean closed = new AtomicBoolean(false);
    ModuleWithLifecycledKeys lifecycleModule = new ModuleWithLifecycledKeys(created, closed);
    Service service = ServiceImpl.builder("test")
        .withModule(lifecycleModule)
        .build();

    try (Service.Instance instance = service.start(new String[]{}, env)) {
      ModuleWithLifecycledKeys.Foo foo =
          instance.resolve(ModuleWithLifecycledKeys.Foo.class);
      assertNotNull(foo);

      instance.getSignaller().signalShutdown();
      instance.waitForShutdown();
    }
  }

  @ParameterizedTest
  @MethodSource("env")
  public void testResolveInvalid(Map<String, String> env) {
    assertThrows(ApolloConfigurationException.class, () -> {
      final Class<?> unusedClass = IntMath.class;

      Service service = ServiceImpl.builder("test").build();
      try (Service.Instance instance = service.start(new String[]{}, env)) {
        instance.resolve(unusedClass);
      }
    });
  }

  @ParameterizedTest
  @MethodSource("env")
  @Timeout(1000)
  public void testCleanShutdown(Map<String, String> env) throws Exception {
    Runtime runtime = mock(Runtime.class);
    Service service = ServiceImpl.builder("test")
        .withRuntime(runtime)
        .build();

    //noinspection EmptyTryBlock
    try (Service.Instance ignored = service.start(new String[]{}, env)) {
      // Do nothing
    }

    // Simulate JVM shutdown hook
    ArgumentCaptor<Thread> threadCaptor = ArgumentCaptor.forClass(Thread.class);

    verify(runtime).addShutdownHook(threadCaptor.capture());

    // Should not block for more than 1 sec (will be interrupted by test timeout if it blocks)
    threadCaptor.getValue().run();
  }

  @ParameterizedTest
  @MethodSource("env")
  public void testHasExecutors(Map<String, String> env) throws Exception {
    Service service = ServiceImpl.builder("test")
        .build();

    try (Service.Instance instance = service.start(new String[]{}, env)) {
      assertNotNull(instance.getExecutorService());
      assertNotNull(instance.getScheduledExecutorService());
    }
  }

  @ParameterizedTest
  @Timeout(1000)
  @MethodSource("env")
  public void testExceptionDuringInit(Map<String, String> env) {
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

    try (Service.Instance instance = service.start(new String[]{}, env)) {
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

  @ParameterizedTest
  @Timeout(1000)
  @MethodSource("env")
  public void testSimulateCtrlCInterrupted(Map<String, String> env) {
    assertThrows(InterruptedException.class, () -> {
      final Runtime runtime = mock(Runtime.class);
      Service service = ServiceImpl.builder("test")
          .withRuntime(runtime)
          .withShutdownInterrupt(true)
          .build();
      ExecutorService executorService = Executors.newSingleThreadExecutor();

      try (Service.Instance instance = service.start(new String[]{}, env)) {
        executorService.submit(new ShutdownHookSim(runtime));
        new CountDownLatch(1).await();
      } finally {
        executorService.shutdownNow();
      }
    });
  }

  @Timeout(1000)
  @ParameterizedTest
  @MethodSource("env")
  public void testSimulateCtrlCWaitingForShutdown(Map<String, String> env) throws Exception {
    final Runtime runtime = mock(Runtime.class);
    Service service = ServiceImpl.builder("test")
        .withRuntime(runtime)
        .build();

    ExecutorService executorService = Executors.newSingleThreadExecutor();

    try (Service.Instance instance = service.start(new String[]{}, env)) {
      executorService.submit(new ShutdownHookSim(runtime));
      instance.waitForShutdown();
    } catch (Throwable ex) {
      fail("Not clean shutdown");
    } finally {
      executorService.shutdownNow();
    }
  }

  @ParameterizedTest
  @MethodSource("env")
  public void testExtraModules(Map<String, String> env) throws Exception {
    AtomicInteger count = new AtomicInteger(0);
    Service service = ServiceImpl.builder("test")
        .withModule(new CountingModuleWithPriority(0.0, count))
        .build();

    try (Service.Instance instance = service.start(
        new String[0], env)) {
      instance.getSignaller().signalShutdown();
      instance.waitForShutdown();
    }
    assertThat(count.get(), is(1));
  }

  @ParameterizedTest
  @MethodSource("env")
  public void testExtraModulesAreDeduped(Map<String, String> env) throws Exception {
    AtomicInteger count = new AtomicInteger(0);

    Service service = ServiceImpl.builder("test")
        .withModule(new CountingModuleWithPriority(0.0, count))
        .build();

    try (Service.Instance instance =
             service.start(
                 new String[0],
                 envAsConfig(env),
                 ImmutableSet.of(new CountingModuleWithPriority(0.0, count),
                                 new CountingModuleWithPriority(0.0, count)))) {

      instance.getSignaller().signalShutdown();
      instance.waitForShutdown();
    }
    assertThat(count.get(), is(1));
  }

  @ParameterizedTest
  @MethodSource("env")
  public void testExtraModulesAreDedupedButNotOnesLoadedWithModule(Map<String, String> env)
      throws Exception {
    AtomicInteger count = new AtomicInteger(0);

    Service service = ServiceImpl.builder("test")
        .withModule(new CountingModuleWithPriority(0.0, count))
        .withModule(new CountingModuleWithPriority(0.0, count))
        .build();

    try (Service.Instance instance = service.start(
        new String[0], envAsConfig(env),
        ImmutableSet.of(new CountingModuleWithPriority(0.0, count)))) {

      instance.getSignaller().signalShutdown();
      instance.waitForShutdown();
    }
    assertThat(count.get(), is(2));
  }

  @org.junit.jupiter.api.Test
  @Timeout(1000)
  public void testSpringBeansAreAvailableWhenSpringIsEnabled()
      throws IOException, InterruptedException {
    Service service = ServiceImpl.builder("test")
        .build();
    try (Service.Instance instance = service.start(new String[]{"-Dapollo.spring.enabled=true"})) {
      assertThat("Should have Spring enabled",
                 instance.getConfig().hasPath("apollo.spring.enabled") && instance.getConfig()
                     .getBoolean("apollo.spring.enabled"));
      assertThat("Should find configured Spring beans",
                 instance.resolve(TestBean.class) != null);
      instance.getSignaller().signalShutdown();
      instance.waitForShutdown();
    }
  }

  @org.junit.jupiter.api.Test
  @Timeout(1000)
  public void testSpringBeansAreAvailableWhenSpringIsEnabledViaSystemEnvironment()
      throws IOException, InterruptedException {
    Service service = ServiceImpl.builder("test")
        .build();
    try (Service.Instance instance = service.start(new String[]{},
                                                   ImmutableMap.of("APOLLO_SPRING_ENABLED",
                                                                   "true"))) {
      assertThat("Should find configured Spring beans",
                 instance.resolve(TestBean.class) != null);
      instance.getSignaller().signalShutdown();
      instance.waitForShutdown();
    }
  }

  @ParameterizedTest
  @Timeout(1000)
  @ValueSource(strings = {"", "-Dapollo.spring.enabled=false"})
  public void testSpringBeansAreNotAvailableWhenSpringIsNotEnabled(String args)
      throws IOException, InterruptedException {
    Service service = ServiceImpl.builder("test")
        .build();

    try (Service.Instance instance = service.start(new String[]{args})) {
      assertThat("Should not have Spring enabled",
                 !instance.getConfig().hasPath("apollo.spring.enabled") ||
                 !instance.getConfig().getBoolean("apollo.spring.enabled"));

      assertThrows(ApolloConfigurationException.class,
                   () -> instance.resolve(TestBean.class));
      instance.getSignaller().signalShutdown();
      instance.waitForShutdown();
    }
  }

  static class Shutdowner implements Callable<Void> {

    private final Service.Signaller signaller;

    public Shutdowner(Service.Signaller signaller) {
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

    public ShutdownHookSim(Runtime runtime) {
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

  public static class TestBean {

    final String test = "woop";
  }

  @Configuration
  static class TestConfiguration {

    @Bean("testSpringBean")
    public TestBean testSpringBean() {
      return new TestBean();
    }
  }

}
