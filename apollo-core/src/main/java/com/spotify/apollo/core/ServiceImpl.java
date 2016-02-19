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

import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.google.common.io.Closer;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.ConfigurationException;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.name.Names;

import com.spotify.apollo.module.ApolloModule;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import joptsimple.BuiltinHelpFormatter;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import static com.google.common.collect.ImmutableList.of;
import static com.google.common.collect.Iterables.concat;
import static com.spotify.apollo.core.Services.CommonConfigKeys;

class ServiceImpl implements Service {

  private static final Logger LOG = LoggerFactory.getLogger(ServiceImpl.class);
  // NOTE: assumes that there is 3 levels between OFF and the default logging level.  The
  // alternative is to somehow let the logging lib inform what this number should be...
  private static final int LOGGING_OFF_OFFSET = -3;
  private static final String ENV_VAR_PREFIX = "APOLLO";

  private final String serviceName;
  private final ImmutableSet<ApolloModule> modules;
  private final String envVarPrefix;
  private final long watchdogTimeout;
  private final TimeUnit watchdogTimeoutUnit;
  private final Runtime runtime;
  private final boolean moduleDiscovery;
  private final boolean shutdownInterrupt;
  private final boolean cliHelp;

  ServiceImpl(
      String serviceName, ImmutableSet<ApolloModule> modules, String envVarPrefix,
      long watchdogTimeout, TimeUnit watchdogTimeoutUnit, Runtime runtime,
      boolean moduleDiscovery, boolean shutdownInterrupt, boolean cliHelp) {
    this.envVarPrefix = envVarPrefix;
    this.watchdogTimeout = watchdogTimeout;
    this.watchdogTimeoutUnit = watchdogTimeoutUnit;
    this.serviceName = Objects.requireNonNull(serviceName);
    this.modules = Objects.requireNonNull(modules);
    this.runtime = runtime;
    this.moduleDiscovery = moduleDiscovery;
    this.shutdownInterrupt = shutdownInterrupt;
    this.cliHelp = cliHelp;
  }

  public String getServiceName() {
    return serviceName;
  }

  public Instance start(String[] args) throws IOException {
    return start(args, System.getenv());
  }

  public Instance start(String[] args, Map<String, String> env) throws IOException {
    return start(args, ConfigFactory.load(serviceName), env);
  }

  public Instance start(final String[] args, final Config config) throws IOException {
    return start(args, config, System.getenv());
  }

  private Instance start(String[] args, Config serviceConfig, Map<String, String> env)
      throws IOException {
    final Closer closer = Closer.create();

    final CountDownLatch shutdownRequested = new CountDownLatch(1);
    final AtomicBoolean started = new AtomicBoolean(false);
    final CountDownLatch stopped = new CountDownLatch(1);
    final Thread threadToInterrupt = shutdownInterrupt ? Thread.currentThread() : null;

    final SignallerImpl signaller = new SignallerImpl(shutdownRequested, threadToInterrupt);

    // If the user presses Ctrl+C at any point, ensure safe clean-up
    runtime.addShutdownHook(
        new Thread(new Reaper(signaller, started, stopped, watchdogTimeout, watchdogTimeoutUnit),
                   serviceName + "-reaper"));

    try {
      final ImmutableList.Builder<String> unprocessedArgsBuilder = ImmutableList.builder();
      Config parsedArguments = parseArgs(
          serviceConfig, args, cliHelp, unprocessedArgsBuilder);
      final ImmutableList<String> unprocessedArgs = unprocessedArgsBuilder.build();

      final Config config = addEnvOverrides(env, parsedArguments).resolve();

      final ListeningExecutorService executorService =
          createExecutorService(closer);

      final ListeningScheduledExecutorService scheduledExecutorService =
          createScheduledExecutorService(closer);

      final Set<ApolloModule> allModules = discoverAllModules();
      final CoreModule coreModule =
          new CoreModule(this, config, signaller, closer, unprocessedArgs);

      final InstanceImpl instance = initInstance(
          coreModule, allModules, closer, executorService,
          scheduledExecutorService, shutdownRequested,
          stopped);

      started.set(true);
      return instance;
    } catch (Throwable e) {
      try {
        closer.close();
      } catch (Throwable ex) {
        e.addSuppressed(ex);
      }
      throw e;
    }
  }

  Config addEnvOverrides(Map<String, String> env, Config config) {
    for (Map.Entry<String, String> var : env.entrySet()) {
      String envKey = var.getKey();
      if (envKey.startsWith(envVarPrefix + "_")) {
        String configKey = envKey.substring(envVarPrefix.length())
            .toLowerCase()
            .replaceAll("(?<!_)_(?!_(__)*([^_]|$))", ".")
            .replaceAll("__", "_")
            .substring(1);
        config = config.withValue(
            configKey, ConfigValueFactory
                .fromAnyRef(var.getValue(), "Environment variable " + envKey));
      }
    }
    return config;
  }

  InstanceImpl initInstance(
      CoreModule coreModule,
      Set<ApolloModule> modules,
      Closer closer,
      ListeningExecutorService executorService,
      ListeningScheduledExecutorService scheduledExecutorService,
      CountDownLatch shutdownRequested,
      CountDownLatch stopped) {
    List<ApolloModule> modulesSortedOnPriority = FluentIterable.from(modules)
        .toSortedList(
            Ordering.natural()
                .reverse()
                .onResultOf(ModulePriorityOrdering.INSTANCE));

    Iterable<Module> allModules = concat(of(coreModule), modulesSortedOnPriority);
    Injector injector = Guice.createInjector(Stage.PRODUCTION, allModules);

    Set<Key<?>> keysToLoad = Sets.newLinkedHashSet();
    for (ApolloModule apolloModule : modulesSortedOnPriority) {
      LOG.info("Loaded module {}", apolloModule);
      keysToLoad.addAll(apolloModule.getLifecycleManaged());
    }

    for (Key<?> key : keysToLoad) {
      Object obj = injector.getInstance(key);
      if (Closeable.class.isAssignableFrom(obj.getClass())) {
        LOG.info("Managing lifecycle of {}", key.getTypeLiteral());
        closer.register(Closeable.class.cast(obj));
      }
    }

    return new InstanceImpl(
        injector, executorService, scheduledExecutorService,
        shutdownRequested, stopped);
  }

  Set<ApolloModule> discoverAllModules() {
    final Set<ApolloModule> allModules;

    if (moduleDiscovery) {
      allModules = Sets.union(modules, ImmutableSet.copyOf(ServiceLoader.load(ApolloModule.class)));
    } else {
      allModules = modules;
    }
    return allModules;
  }

  ListeningScheduledExecutorService createScheduledExecutorService(Closer closer) {
    final ListeningScheduledExecutorService scheduledExecutorService =
        MoreExecutors.listeningDecorator(
            Executors.newScheduledThreadPool(
                Math.max(Runtime.getRuntime().availableProcessors(), 2),
                new ThreadFactoryBuilder().setNameFormat(serviceName + "-scheduled-%d").build()));
    closer.register(asCloseable(scheduledExecutorService));
    return scheduledExecutorService;
  }

  ListeningExecutorService createExecutorService(Closer closer) {
    final ListeningExecutorService executorService =
        MoreExecutors.listeningDecorator(
            Executors.newCachedThreadPool(
                new ThreadFactoryBuilder().setNameFormat(serviceName + "-worker-%d").build()));
    closer.register(asCloseable(executorService));
    return executorService;
  }

  Closeable asCloseable(final ExecutorService executorService) {
    return new ExecutorServiceCloseable(executorService);
  }

  static Config parseArgs(
      Config config, String[] args, boolean cliHelp,
      ImmutableList.Builder<String> unprocessedArgsBuilder) throws IOException {

    config = appendConfig(
        config,
        CommonConfigKeys.APOLLO_ARGS_CORE.getKey(), Arrays.asList(args),
        "apollo cli args");

    final OptionParser parser = new OptionParser();

    parser.formatHelpWith(new BuiltinHelpFormatter(1024, 2));
    parser.allowsUnrecognizedOptions();
    parser.posixlyCorrect(System.getenv("POSIXLY_CORRECT") != null);
    parser.recognizeAlternativeLongOptions(true);

    final OptionSpec<Void> helpOption;
    if (cliHelp) {
      helpOption =
          parser.acceptsAll(ImmutableList.of("help", "h"), "Shows this help.").forHelp();
    } else {
      helpOption = null;
    }

    final OptionSpec<String> configOption = parser.accepts(
        "D",
        "Set configuration key with '-Dkey=value'.  Supports Typesafe Config syntax, i.e. "
        + "'-Dhosts+=foo.${domain}'.")
        .withRequiredArg();

    final OptionSpec<Boolean> syslogOption = parser.accepts(
        "syslog",
        String.format(
            "Log to syslog (Alias for '-D%s=<value>').",
            CommonConfigKeys.LOGGING_SYSLOG.getKey()))
        .withOptionalArg().ofType(Boolean.class);

    final OptionSpec<Void> verboseOption = parser.acceptsAll(
        ImmutableList.of("verbose", "v"),
        String.format(
            "Increase logging verbosity.  Overrides config key '%s'.",
            CommonConfigKeys.LOGGING_VERBOSITY.getKey()));

    final OptionSpec<Void> conciseOption = parser.acceptsAll(
        ImmutableList.of("concise", "c"),
        String.format(
            "Decrease logging verbosity.  Overrides config key '%s'.",
            CommonConfigKeys.LOGGING_VERBOSITY.getKey()));

    final OptionSpec<Void> quietOption = parser.acceptsAll(
        ImmutableList.of("quiet", "q"),
        String.format(
            "Resets logging level to OFF.  Can be mixed with '-v'/'--verbose' or "
            + "'-c'/'--concise'.  Overrides config key '%s'.",
            CommonConfigKeys.LOGGING_VERBOSITY.getKey()));

    final OptionSpec<String> configFile = parser.accepts(
        "config",
        "Load configuration from the specified file. The values from the file will be "
        + "overlayed on top of any already loaded configuration.")
        .withRequiredArg();

    final OptionSpec<String> unparsedOption =
        parser
            .nonOptions("Service-specific options that will be passed to the underlying service.");

    final OptionSet parsed;
    try {
      parsed = parser.parse(args);
    } catch (OptionException e) {
      throw new ApolloCliException("Could not parse command-line arguments", e);
    }

    if (helpOption != null && parsed.has(helpOption)) {
      // TODO: make help output a bit prettier
      StringWriter stringWriter = new StringWriter();

      try (PrintWriter pw = new PrintWriter(stringWriter)) {
        pw.println();
        pw.println("Usage: <program> [options...] -- [non-option args...]");
        pw.println();
        parser.printHelpOn(pw);
      }

      throw new ApolloHelpException(stringWriter.toString());
    }

    unprocessedArgsBuilder.addAll(parsed.valuesOf(unparsedOption));
    config = appendConfig(
        config,
        CommonConfigKeys.APOLLO_ARGS_UNPARSED.getKey(),
        unprocessedArgsBuilder.build(), "apollo unparsed cli args");

    int verbosity = 0;
    boolean hasVerbosity = false;
    for (OptionSpec<?> optionSpec : parsed.specs()) {
      if (optionSpec == quietOption) {
        verbosity = LOGGING_OFF_OFFSET;
        hasVerbosity = true;
      } else if (optionSpec == verboseOption) {
        verbosity++;
        hasVerbosity = true;
      } else if (optionSpec == conciseOption) {
        verbosity--;
        hasVerbosity = true;
      }
    }

    if (hasVerbosity) {
      config = appendConfig(
          config,
          CommonConfigKeys.LOGGING_VERBOSITY.getKey(), verbosity,
          "Command-line verbosity flags");
    }

    if (parsed.has(syslogOption)) {
      final boolean syslog;

      if (parsed.hasArgument(syslogOption)) {
        syslog = parsed.valueOf(syslogOption);
      } else {
        syslog = true;
      }

      config = appendConfig(
          config,
          CommonConfigKeys.LOGGING_SYSLOG.getKey(), syslog,
          "Command-line option --syslog");
    }

    for (String configString : parsed.valuesOf(configOption)) {
      String[] parts = configString.split("=", 2);

      final String key;
      final Object value;
      if (parts.length == 2) {
        key = parts[0];
        value = parts[1];
      } else {
        key = parts[0];
        value = true;
      }
      config = appendConfig(
          config,
          key, value,
          "Command-line configuration -D" + parts[0]);
    }

    if (parsed.has(configFile)) {
      final String configFileValue = parsed.valueOf(configFile);
      final Config overlayConfig = ConfigFactory.parseFile(new File(configFileValue));
      config = overlayConfig.withFallback(config);
    }

    return config;
  }

  static Config appendConfig(Config config, String key, Object value, String description) {
    return config.withValue(key, ConfigValueFactory.fromAnyRef(value, description));
  }

  static Builder builder(String serviceName) {
    return new BuilderImpl(
        serviceName, ImmutableSet.builder(), ENV_VAR_PREFIX, 1, TimeUnit.MINUTES, Runtime.getRuntime(),
        false, false, true);
  }

  static class BuilderImpl implements Builder {

    private final String serviceName;
    private final ImmutableSet.Builder<ApolloModule> moduleBuilder;
    private String envVarPrefix;
    private long watchdogTimeout;
    private TimeUnit watchdogTimeoutUnit;
    private Runtime runtime;
    private boolean moduleDiscovery;
    private boolean shutdownInterrupt;
    private boolean cliHelp;

    BuilderImpl(
        String serviceName,
        ImmutableSet.Builder<ApolloModule> moduleBuilder,
        String envVarPrefix,
        long watchdogTimeout, TimeUnit watchdogTimeoutUnit, Runtime runtime,
        boolean moduleDiscovery,
        boolean shutdownInterrupt,
        boolean cliHelp) {
      this.serviceName = Objects.requireNonNull(serviceName);
      this.moduleBuilder = moduleBuilder;
      this.envVarPrefix = envVarPrefix;
      this.watchdogTimeout = watchdogTimeout;
      this.watchdogTimeoutUnit = watchdogTimeoutUnit;
      this.runtime = runtime;
      this.moduleDiscovery = moduleDiscovery;
      this.shutdownInterrupt = shutdownInterrupt;
      this.cliHelp = cliHelp;
    }

    @Override
    public Builder withShutdownInterrupt(boolean enabled) {
      this.shutdownInterrupt = enabled;
      return this;
    }

    @Override
    public Builder withCliHelp(boolean enabled) {
      this.cliHelp = enabled;
      return this;
    }

    @Override
    public Builder withEnvVarPrefix(String prefix) {
      this.envVarPrefix = prefix;
      return this;
    }

    @Override
    public Builder withWatchdogTimeout(long timeout, TimeUnit unit) {
      watchdogTimeout = timeout;
      watchdogTimeoutUnit = unit;
      return this;
    }

    @Override
    public Builder withRuntime(Runtime runtime) {
      this.runtime = runtime;
      return this;
    }

    @Override
    public Builder withModule(ApolloModule module) {
      moduleBuilder.add(module);
      return this;
    }

    @Override
    public Builder usingModuleDiscovery(boolean moduleDiscovery) {
      this.moduleDiscovery = moduleDiscovery;
      return this;
    }

    @Override
    public Service build() {
      return new ServiceImpl(
          serviceName, moduleBuilder.build(), envVarPrefix, watchdogTimeout, watchdogTimeoutUnit,
          runtime, moduleDiscovery, shutdownInterrupt, cliHelp);
    }
  }

  static class ExecutorServiceCloseable implements Closeable {

    private final ExecutorService executorService;

    ExecutorServiceCloseable(ExecutorService executorService) {
      this.executorService = executorService;
    }

    @Override
    public void close() throws IOException {
      executorService.shutdown();

      boolean terminated;
      try {
        terminated = executorService.awaitTermination(10, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        terminated = false;
      }

      if (!terminated) {
        executorService.shutdownNow();
      }
    }
  }

  enum ModulePriorityOrdering implements Function<ApolloModule, Comparable> {
    INSTANCE;

    @Override
    public Comparable apply(ApolloModule input) {
      return input.getPriority();
    }
  }

  static class Reaper implements Runnable {

    private final Signaller signaller;
    private final AtomicBoolean started;
    private final CountDownLatch stopped;
    private final long watchdogTimeout;
    private final TimeUnit watchdogTimeoutUnit;

    public Reaper(Signaller signaller, AtomicBoolean started, CountDownLatch stopped,
                  long watchdogTimeout,
                  TimeUnit watchdogTimeoutUnit) {
      this.signaller = signaller;
      this.started = started;
      this.stopped = stopped;
      this.watchdogTimeout = watchdogTimeout;
      this.watchdogTimeoutUnit = watchdogTimeoutUnit;
    }

    @Override
    public void run() {
      if (started.get()) {
        signaller.signalShutdown();
        try {
          // The service gets some time to shut down.  We can't wait forever here because that
          // will dead-lock the JVM, forcing us to SIGKILL
          stopped.await(watchdogTimeout, watchdogTimeoutUnit);
        } catch (InterruptedException e) {
          LOG.error("Interrupted while doing Apollo shutdown", e);
          Thread.currentThread().interrupt();
        }
      }
    }
  }

  class InstanceImpl implements Instance {

    private final Injector injector;
    private final ListeningExecutorService executorService;
    private final ListeningScheduledExecutorService scheduledExecutorService;
    private final CountDownLatch shutdownRequested;
    private final CountDownLatch stopped;

    InstanceImpl(
        Injector injector, ListeningExecutorService executorService,
        ListeningScheduledExecutorService scheduledExecutorService,
        CountDownLatch shutdownRequested, CountDownLatch stopped) {
      this.injector = injector;
      this.executorService = executorService;
      this.scheduledExecutorService = scheduledExecutorService;
      this.shutdownRequested = shutdownRequested;
      this.stopped = stopped;
    }

    @Override
    public Service getService() {
      return ServiceImpl.this;
    }

    @Override
    public Config getConfig() {
      return resolve(Config.class);
    }

    @Override
    public ListeningExecutorService getExecutorService() {
      return executorService;
    }

    @Override
    public ListeningScheduledExecutorService getScheduledExecutorService() {
      return scheduledExecutorService;
    }

    @Override
    public Closer getCloser() {
      return resolve(Closer.class);
    }

    @Override
    public Signaller getSignaller() {
      return resolve(Signaller.class);
    }

    @Override
    public ImmutableList<String> getUnprocessedArgs() {
      return injector.getInstance(CoreModule.UNPROCESSED_ARGS);
    }

    @Override
    public <T> T resolve(Class<T> type) {
      try {
        return injector.getInstance(type);
      } catch (ConfigurationException ex) {
        throw new ApolloConfigurationException("Can't find instance of type " + type.getName(), ex);
      }
    }

    @Override
    public void waitForShutdown() throws InterruptedException {
      shutdownRequested.await();
    }

    @Override
    public boolean isShutdown() {
      return shutdownRequested.getCount() == 0l;
    }

    @Override
    public void close() throws IOException {
      try {
        getCloser().close();
      } finally {
        stopped.countDown();
      }
    }

    @Override
    public String toString() {
      return "Service";
    }
  }

  static class CoreModule extends AbstractModule {

    public static final Key<ImmutableList<String>> UNPROCESSED_ARGS =
        new Key<ImmutableList<String>>(Names.named(Services.INJECT_UNPROCESSED_ARGS)) {
        };

    public static final Key<String> SERVICE_NAME =
        new Key<String>(Names.named(Services.INJECT_SERVICE_NAME)) {
        };

    private final ServiceImpl service;
    private final Config config;
    private final Signaller signaller;
    private final Closer closer;
    private final ImmutableList<String> unprocessedArgs;

    CoreModule(
        ServiceImpl service, Config config, Signaller signaller,
        Closer closer, ImmutableList<String> unprocessedArgs) {
      this.service = service;
      this.config = config;
      this.signaller = signaller;
      this.closer = closer;
      this.unprocessedArgs = unprocessedArgs;
    }

    @Override
    protected void configure() {
      // Guice will happily construct objects of concrete classes with a default constructor to
      // satisfy such injections. This option will disable the implicit bindings and require them
      // to be explicitly bound. Since this module is always loaded, we'll always require explicit
      // bindings for all other module.
      binder().requireExplicitBindings();

      bind(Service.class).toInstance(service);
      bind(Config.class).toInstance(config);
      bind(Signaller.class).toInstance(signaller);
      bind(Closer.class).toInstance(closer);
      bind(SERVICE_NAME).toInstance(service.getServiceName());
      bind(UNPROCESSED_ARGS).toInstance(unprocessedArgs);
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("id", "apollo")
          .toString();
    }
  }

  static class SignallerImpl implements Signaller {

    private final CountDownLatch shutdownRequested;
    private final Thread threadToInterrupt;

    SignallerImpl(CountDownLatch shutdownRequested, Thread threadToInterrupt) {
      this.shutdownRequested = shutdownRequested;
      this.threadToInterrupt = threadToInterrupt;
    }

    @Override
    public void signalShutdown() {
      shutdownRequested.countDown();
      if (threadToInterrupt != null) {
        threadToInterrupt.interrupt();
      }
    }
  }
}
