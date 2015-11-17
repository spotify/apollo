package com.spotify.apollo.core;

import com.google.common.collect.ImmutableMap;
import com.google.common.math.IntMath;

import com.spotify.apollo.module.AbstractApolloModule;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.spotify.apollo.core.Services.CommonConfigKeys.APOLLO_ARGS_CORE;
import static com.spotify.apollo.core.Services.CommonConfigKeys.APOLLO_ARGS_UNPARSED;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ServiceImplTest {

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
  public void testConfig() throws Exception {
    Service service = ServiceImpl.builder("test").build();

    try (Service.Instance instance = service.start("-Dconfig=value")) {
      assertThat(instance.getUnprocessedArgs(), is(empty()));
      assertThat(instance.getConfig().getString("config"), is("value"));
    }
  }

  @Test
  public void testEnvConfig() throws Exception {
    Service service = ServiceImpl.builder("test").build();

    try (Service.Instance instance = service
        .start(new String[]{}, ImmutableMap.of("SPOTIFY_A_B", "value"))) {
      assertThat(instance.getConfig().getString("a.b"), is("value"));
    }
  }

  @Test
  public void testEnvConfigWithLeadingUnderscores() throws Exception {
    Service service = ServiceImpl.builder("test").build();

    try (Service.Instance instance = service
        .start(new String[]{}, ImmutableMap.of("SPOTIFY___A_B", "value"))) {
      assertThat(instance.getConfig().getString("_a.b"), is("value"));
    }
  }

  @Test
  public void testEnvConfigWithUnderscoresEverywhere() throws Exception {
    Service service = ServiceImpl.builder("test").build();

    try (Service.Instance instance = service
        .start(new String[]{}, ImmutableMap.of("SPOTIFY_A___B__C______D_____E__", "value"))) {
      assertThat(instance.getConfig().getString("a._b_c___d.__e_"), is("value"));
    }
  }

  @Test
  public void testUsesConfig() throws IOException {
    Service service = ServiceImpl.builder("test").build();

    Config config = ConfigFactory.empty()
        .withValue("this.key", ConfigValueFactory.fromAnyRef("value for this.key"));

    try (Service.Instance instance = service.start(new String[]{}, config)) {
      assertThat(instance.getConfig().getString("this.key"), is("value for this.key"));
    }
  }

  @Test
  public void testConfigSupportsColonValues() throws Exception {
    Service service = ServiceImpl.builder("test").build();

    try (Service.Instance instance = service.start("-Dconfig=value:more")) {
      assertThat(instance.getUnprocessedArgs(), is(empty()));
      assertThat(instance.getConfig().getString("config"), is("value:more"));
    }
  }

  @Test
  public void testConfigSupportsEqualsValues() throws Exception {
    Service service = ServiceImpl.builder("test").build();

    try (Service.Instance instance = service.start("-Dconfig=value=more")) {
      assertThat(instance.getUnprocessedArgs(), is(empty()));
      assertThat(instance.getConfig().getString("config"), is("value=more"));
    }
  }

  @Test
  public void testUnresolved() throws Exception {
    Service service = ServiceImpl.builder("test").build();

    try (Service.Instance instance = service.start("-Dfoo=bar", "hello")) {
      assertThat(instance.getUnprocessedArgs(), contains("hello"));
      assertThat(instance.getConfig().getString("foo"), is("bar"));
    }
  }

  @Test
  public void testArgsDone() throws Exception {
    Service service = ServiceImpl.builder("test").build();

    try (Service.Instance instance = service.start("-Dfoo=bar", "--", "xyz", "-Dbar=baz", "abc")) {
      assertThat(instance.getUnprocessedArgs(), contains("xyz", "-Dbar=baz", "abc"));
      assertThat(instance.getConfig().getString("foo"), is("bar"));
      assertThat(instance.getConfig().hasPath("bar"), is(false));
    }
  }

  @Test
  public void testArgsAreInConfig() throws Exception {
    Service service = ServiceImpl.builder("test").build();

    try (Service.Instance instance = service.start("-Dfoo=bar", "--", "xyz", "-Dbar=baz", "abc")) {
      assertThat(instance.getConfig().hasPath(APOLLO_ARGS_CORE.getKey()), is(true));
      assertThat(instance.getConfig().hasPath(APOLLO_ARGS_UNPARSED.getKey()), is(true));
      assertThat(instance.getConfig().getStringList(APOLLO_ARGS_CORE.getKey()),
                 contains("-Dfoo=bar", "--", "xyz", "-Dbar=baz", "abc"));
      assertThat(instance.getConfig().getStringList(APOLLO_ARGS_UNPARSED.getKey()),
                 contains("xyz", "-Dbar=baz", "abc"));
    }
  }

  @Test
  public void testVerbosity() throws Exception {
    Service service = ServiceImpl.builder("test").build();

    try (Service.Instance instance = service.start("-v")) {
      assertThat(instance.getUnprocessedArgs(), is(empty()));
      assertThat(instance.getConfig().getInt("logging.verbosity"), is(1));
    }
  }

  @Test
  public void testVerbosityLong() throws Exception {
    Service service = ServiceImpl.builder("test").build();

    try (Service.Instance instance = service.start("--verbose")) {
      assertThat(instance.getUnprocessedArgs(), is(empty()));
      assertThat(instance.getConfig().getInt("logging.verbosity"), is(1));
    }
  }

  @Test
  public void testVerbosityLongMany() throws Exception {
    Service service = ServiceImpl.builder("test").build();

    try (Service.Instance instance = service.start("--verbose", "--verbose")) {
      assertThat(instance.getUnprocessedArgs(), is(empty()));
      assertThat(instance.getConfig().getInt("logging.verbosity"), is(2));
    }
  }

  @Test
  public void testVerbosityStacked() throws Exception {
    Service service = ServiceImpl.builder("test").build();

    try (Service.Instance instance = service.start("-vvvv")) {
      assertThat(instance.getUnprocessedArgs(), is(empty()));
      assertThat(instance.getConfig().getInt("logging.verbosity"), is(4));
    }
  }

  @Test
  public void testVerbosityMany() throws Exception {
    Service service = ServiceImpl.builder("test").build();

    try (Service.Instance instance = service.start("-vv", "-vv")) {
      assertThat(instance.getUnprocessedArgs(), is(empty()));
      assertThat(instance.getConfig().getInt("logging.verbosity"), is(4));
    }
  }

  @Test
  public void testVerbosityQuiet() throws Exception {
    Service service = ServiceImpl.builder("test").build();

    try (Service.Instance instance = service.start("-q")) {
      assertThat(instance.getUnprocessedArgs(), is(empty()));
      assertThat(instance.getConfig().getInt("logging.verbosity"), is(-3));
    }
  }

  @Test
  public void testVerbosityQuietLong() throws Exception {
    Service service = ServiceImpl.builder("test").build();

    try (Service.Instance instance = service.start("--quiet")) {
      assertThat(instance.getUnprocessedArgs(), is(empty()));
      assertThat(instance.getConfig().getInt("logging.verbosity"), is(-3));
    }
  }

  @Test
  public void testVerbosityQuietThenVerbose() throws Exception {
    Service service = ServiceImpl.builder("test").build();

    try (Service.Instance instance = service.start("--quiet", "--verbose")) {
      assertThat(instance.getUnprocessedArgs(), is(empty()));
      assertThat(instance.getConfig().getInt("logging.verbosity"), is(-2));
    }
  }

  @Test
  public void testVerbosityConcise() throws Exception {
    Service service = ServiceImpl.builder("test").build();

    try (Service.Instance instance = service.start("-c")) {
      assertThat(instance.getUnprocessedArgs(), is(empty()));
      assertThat(instance.getConfig().getInt("logging.verbosity"), is(-1));
    }
  }

  @Test
  public void testVerbosityConciseLong() throws Exception {
    Service service = ServiceImpl.builder("test").build();

    try (Service.Instance instance = service.start("--concise")) {
      assertThat(instance.getUnprocessedArgs(), is(empty()));
      assertThat(instance.getConfig().getInt("logging.verbosity"), is(-1));
    }
  }

  @Test
  public void testVerbosityConciseLongMany() throws Exception {
    Service service = ServiceImpl.builder("test").build();

    try (Service.Instance instance = service.start("--concise", "--concise")) {
      assertThat(instance.getUnprocessedArgs(), is(empty()));
      assertThat(instance.getConfig().getInt("logging.verbosity"), is(-2));
    }
  }

  @Test
  public void testVerbosityConciseStacked() throws Exception {
    Service service = ServiceImpl.builder("test").build();

    try (Service.Instance instance = service.start("-cccc")) {
      assertThat(instance.getUnprocessedArgs(), is(empty()));
      assertThat(instance.getConfig().getInt("logging.verbosity"), is(-4));
    }
  }

  @Test
  public void testVerbosityConciseMany() throws Exception {
    Service service = ServiceImpl.builder("test").build();

    try (Service.Instance instance = service.start("-cc", "-cc")) {
      assertThat(instance.getUnprocessedArgs(), is(empty()));
      assertThat(instance.getConfig().getInt("logging.verbosity"), is(-4));
    }
  }

  @Test
  public void testSyslog() throws Exception {
    Service service = ServiceImpl.builder("test").build();

    try (Service.Instance instance = service.start("--syslog")) {
      assertThat(instance.getUnprocessedArgs(), is(empty()));
      assertThat(instance.getConfig().getBoolean("logging.syslog"), is(true));
    }
  }

  @Test
  public void testSyslogTrue() throws Exception {
    Service service = ServiceImpl.builder("test").build();

    try (Service.Instance instance = service.start("--syslog=true")) {
      assertThat(instance.getUnprocessedArgs(), is(empty()));
      assertThat(instance.getConfig().getBoolean("logging.syslog"), is(true));
    }
  }

  @Test
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

  @Test(timeout = 1000)
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

  @Test
  public void testUnresolvedMixed() throws Exception {
    // Issue #8
    Service service = ServiceImpl.builder("test").build();

    try (Service.Instance instance = service.start("--hello", "-Dfoo=bar", "bye")) {
      assertThat(instance.getUnprocessedArgs(), contains("--hello", "bye"));
      assertThat(instance.getConfig().getString("foo"), is("bar"));
    }
  }

  @Test(timeout = 1000, expected = InterruptedException.class)
  public void testInterrupt() throws Exception {
    Service service = ServiceImpl.builder("test").withShutdownInterrupt(true).build();

    try (Service.Instance instance = service.start()) {
      instance.getScheduledExecutorService()
          .schedule(new Shutdowner(instance.getSignaller()), 5, TimeUnit.MILLISECONDS);
      new CountDownLatch(1).await(); // Wait forever
    }
  }

  @Test
  public void testHelpFallthrough() throws Exception {
    Service service = ServiceImpl.builder("test").withCliHelp(false).build();

    try (Service.Instance instance = service.start("--help", "-h")) {
      assertThat(instance.getUnprocessedArgs(), contains("--help", "-h"));
    }
  }

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

  @Test
  public void testHasExecutors() throws Exception {
    Service service = ServiceImpl.builder("test")
        .build();

    try (Service.Instance instance = service.start()) {
      assertNotNull(instance.getExecutorService());
      assertNotNull(instance.getScheduledExecutorService());
    }
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
}
