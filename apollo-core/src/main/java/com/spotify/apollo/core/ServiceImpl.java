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

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.google.common.io.Closer;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.google.common.collect.ImmutableList.of;
import static com.google.common.collect.Iterables.concat;
import static java.util.Objects.requireNonNull;

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
  private final Function<Config, Config> configDecorator;

  private ServiceImpl(
      String serviceName, ImmutableSet<ApolloModule> modules, String envVarPrefix,
      long watchdogTimeout, TimeUnit watchdogTimeoutUnit, Runtime runtime,
      boolean moduleDiscovery, boolean shutdownInterrupt, boolean cliHelp,
      Function<Config, Config> configDecorator) {
    this.envVarPrefix = envVarPrefix;
    this.watchdogTimeout = watchdogTimeout;
    this.watchdogTimeoutUnit = watchdogTimeoutUnit;
    this.serviceName = requireNonNull(serviceName);
    this.modules = requireNonNull(modules);
    this.runtime = runtime;
    this.moduleDiscovery = moduleDiscovery;
    this.shutdownInterrupt = shutdownInterrupt;
    this.cliHelp = cliHelp;
    this.configDecorator = requireNonNull(configDecorator);
  }

  @Override
  public String getServiceName() {
    return serviceName;
  }

  @Override
  public Instance start(String[] args) throws IOException {
    return start(args, System.getenv());
  }

//  @Override
//  public Instance start(String[] args, Map<String, String> env) throws IOException {
//    return start(args, ConfigFactory.load(serviceName), env);
//  }

//  @Override
//  public Instance start(final String[] args, final Config config) throws IOException {
//    return start(args, config, System.getenv());
//  }

  @Override
  public Instance start(String[] args, Map<String, String> env) throws IOException {
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
//      final ImmutableList.Builder<String> unprocessedArgsBuilder = ImmutableList.builder();
//      Config parsedArguments = parseArgs(
//          serviceConfig, args, cliHelp, unprocessedArgsBuilder);
//      final ImmutableList<String> unprocessedArgs = unprocessedArgsBuilder.build();
//
//      final Config config = addEnvOverrides(env, parsedArguments).resolve();

      final Set<ApolloModule> allModules = discoverAllModules();
      final Config config = configDecorator.apply(ConfigFactory.load());

      final CoreModule coreModule =
          new CoreModule(this, signaller, closer, ImmutableList.copyOf(args), env, config);

      final InstanceImpl instance = initInstance(
          coreModule, allModules, closer,
          shutdownRequested,
          stopped, config);

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

//  private Config addEnvOverrides(Map<String, String> env, Config config) {
//    for (Map.Entry<String, String> var : env.entrySet()) {
//      String envKey = var.getKey();
//      if (envKey.startsWith(envVarPrefix + "_")) {
//        String configKey = envKey.substring(envVarPrefix.length())
//            .toLowerCase()
//            .replaceAll("(?<!_)_(?!_(__)*([^_]|$))", ".")
//            .replaceAll("__", "_")
//            .substring(1);
//        config = config.withValue(
//            configKey, ConfigValueFactory
//                .fromAnyRef(var.getValue(), "Environment variable " + envKey));
//      }
//    }
//    return config;
//  }

  private InstanceImpl initInstance(
      CoreModule coreModule,
      Set<ApolloModule> modules,
      Closer closer,
      CountDownLatch shutdownRequested,
      CountDownLatch stopped,
      Config config) {
    List<ApolloModule> modulesSortedOnPriority = modules.stream()
        .sorted(Ordering.natural()
                    .reverse()
                    .onResultOf(
                        ModulePriorityOrdering.INSTANCE))
        .collect(Collectors.toList());

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
        injector,
        shutdownRequested,
        stopped,
        config);
  }

  private Set<ApolloModule> discoverAllModules() {
    final Set<ApolloModule> allModules;

    if (moduleDiscovery) {
      allModules = Sets.union(modules, ImmutableSet.copyOf(ServiceLoader.load(ApolloModule.class)));
    } else {
      allModules = modules;
    }
    return allModules;
  }

//  private static Config parseArgs(
//      Config config, String[] args, boolean cliHelp,
//      ImmutableList.Builder<String> unprocessedArgsBuilder) throws IOException {
//
//    config = appendConfig(
//        config,
//        CommonConfigKeys.APOLLO_ARGS_CORE.getKey(), Arrays.asList(args),
//        "apollo cli args");
//
//    final OptionParser parser = new OptionParser();
//
//    parser.formatHelpWith(new BuiltinHelpFormatter(1024, 2));
//    parser.allowsUnrecognizedOptions();
//    parser.posixlyCorrect(System.getenv("POSIXLY_CORRECT") != null);
//    parser.recognizeAlternativeLongOptions(true);
//
//    final OptionSpec<Void> helpOption;
//    if (cliHelp) {
//      helpOption =
//          parser.acceptsAll(ImmutableList.of("help", "h"), "Shows this help.").forHelp();
//    } else {
//      helpOption = null;
//    }
//
//    final OptionSpec<String> configOption = parser.accepts(
//        "D",
//        "Set configuration key with '-Dkey=value'.  Supports Typesafe Config syntax, i.e. "
//        + "'-Dhosts+=foo.${domain}'.")
//        .withRequiredArg();
//
//    final OptionSpec<Boolean> syslogOption = parser.accepts(
//        "syslog",
//        String.format(
//            "Log to syslog (Alias for '-D%s=<value>').",
//            CommonConfigKeys.LOGGING_SYSLOG.getKey()))
//        .withOptionalArg().ofType(Boolean.class);
//
//    final OptionSpec<Void> verboseOption = parser.acceptsAll(
//        ImmutableList.of("verbose", "v"),
//        String.format(
//            "Increase logging verbosity.  Overrides config key '%s'.",
//            CommonConfigKeys.LOGGING_VERBOSITY.getKey()));
//
//    final OptionSpec<Void> conciseOption = parser.acceptsAll(
//        ImmutableList.of("concise", "c"),
//        String.format(
//            "Decrease logging verbosity.  Overrides config key '%s'.",
//            CommonConfigKeys.LOGGING_VERBOSITY.getKey()));
//
//    final OptionSpec<Void> quietOption = parser.acceptsAll(
//        ImmutableList.of("quiet", "q"),
//        String.format(
//            "Resets logging level to OFF.  Can be mixed with '-v'/'--verbose' or "
//            + "'-c'/'--concise'.  Overrides config key '%s'.",
//            CommonConfigKeys.LOGGING_VERBOSITY.getKey()));
//
//    final OptionSpec<String> configFile = parser.accepts(
//        "config",
//        "Load configuration from the specified file. The values from the file will be "
//        + "overlayed on top of any already loaded configuration.")
//        .withRequiredArg();
//
//    final OptionSpec<String> unparsedOption =
//        parser
//            .nonOptions("Service-specific options that will be passed to the underlying service.");
//
//    final OptionSet parsed;
//    try {
//      parsed = parser.parse(args);
//    } catch (OptionException e) {
//      throw new ApolloCliException("Could not parse command-line arguments", e);
//    }
//
//    if (helpOption != null && parsed.has(helpOption)) {
//      // TODO: make help output a bit prettier
//      StringWriter stringWriter = new StringWriter();
//
//      try (PrintWriter pw = new PrintWriter(stringWriter)) {
//        pw.println();
//        pw.println("Usage: <program> [options...] -- [non-option args...]");
//        pw.println();
//        parser.printHelpOn(pw);
//      }
//
//      throw new ApolloHelpException(stringWriter.toString());
//    }
//
//    unprocessedArgsBuilder.addAll(parsed.valuesOf(unparsedOption));
//    config = appendConfig(
//        config,
//        CommonConfigKeys.APOLLO_ARGS_UNPARSED.getKey(),
//        unprocessedArgsBuilder.build(), "apollo unparsed cli args");
//
//    int verbosity = 0;
//    boolean hasVerbosity = false;
//    for (OptionSpec<?> optionSpec : parsed.specs()) {
//      if (optionSpec == quietOption) {
//        verbosity = LOGGING_OFF_OFFSET;
//        hasVerbosity = true;
//      } else if (optionSpec == verboseOption) {
//        verbosity++;
//        hasVerbosity = true;
//      } else if (optionSpec == conciseOption) {
//        verbosity--;
//        hasVerbosity = true;
//      }
//    }
//
//    if (hasVerbosity) {
//      config = appendConfig(
//          config,
//          CommonConfigKeys.LOGGING_VERBOSITY.getKey(), verbosity,
//          "Command-line verbosity flags");
//    }
//
//    if (parsed.has(syslogOption)) {
//      final boolean syslog;
//
//      if (parsed.hasArgument(syslogOption)) {
//        syslog = parsed.valueOf(syslogOption);
//      } else {
//        syslog = true;
//      }
//
//      config = appendConfig(
//          config,
//          CommonConfigKeys.LOGGING_SYSLOG.getKey(), syslog,
//          "Command-line option --syslog");
//    }
//
//    for (String configString : parsed.valuesOf(configOption)) {
//      String[] parts = configString.split("=", 2);
//
//      final String key;
//      final Object value;
//      if (parts.length == 2) {
//        key = parts[0];
//        value = parts[1];
//      } else {
//        key = parts[0];
//        value = true;
//      }
//      config = appendConfig(
//          config,
//          key, value,
//          "Command-line configuration -D" + parts[0]);
//    }
//
//    if (parsed.has(configFile)) {
//      final String configFileValue = parsed.valueOf(configFile);
//      final Config overlayConfig = ConfigFactory.parseFile(new File(configFileValue));
//      config = overlayConfig.withFallback(config);
//    }
//
//    return config;
//  }

//  private static Config appendConfig(Config config, String key, Object value, String description) {
//    return config.withValue(key, ConfigValueFactory.fromAnyRef(value, description));
//  }

  static Builder builder(String serviceName) {
    return new BuilderImpl(
        serviceName, ImmutableSet.builder(), ENV_VAR_PREFIX, 1, TimeUnit.MINUTES, Runtime.getRuntime(),
        false, false, true);
  }

  private static class BuilderImpl implements Builder {

    private final String serviceName;
    private final ImmutableSet.Builder<ApolloModule> moduleBuilder;
    private String envVarPrefix;
    private long watchdogTimeout;
    private TimeUnit watchdogTimeoutUnit;
    private Runtime runtime;
    private boolean moduleDiscovery;
    private boolean shutdownInterrupt;
    private boolean cliHelp;
    private Function<Config, Config> configDecorator = config -> config;

    BuilderImpl(
        String serviceName,
        ImmutableSet.Builder<ApolloModule> moduleBuilder,
        String envVarPrefix,
        long watchdogTimeout, TimeUnit watchdogTimeoutUnit, Runtime runtime,
        boolean moduleDiscovery,
        boolean shutdownInterrupt,
        boolean cliHelp) {
      this.serviceName = requireNonNull(serviceName);
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
    public Builder withConfigDecorator(Function<Config, Config> decorator) {
      this.configDecorator = decorator;
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
          runtime, moduleDiscovery, shutdownInterrupt, cliHelp, configDecorator);
    }
  }

  private enum ModulePriorityOrdering implements com.google.common.base.Function<ApolloModule, Comparable> {
    INSTANCE;

    @Override
    public Comparable apply(ApolloModule input) {
      return input.getPriority();
    }
  }

  private static class Reaper implements Runnable {

    private final Signaller signaller;
    private final AtomicBoolean started;
    private final CountDownLatch stopped;
    private final long watchdogTimeout;
    private final TimeUnit watchdogTimeoutUnit;

    Reaper(Signaller signaller, AtomicBoolean started, CountDownLatch stopped,
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

  private class InstanceImpl implements Instance {

    private final Injector injector;
    private final CountDownLatch shutdownRequested;
    private final CountDownLatch stopped;
    private final Config config;

    InstanceImpl(
        Injector injector,
        CountDownLatch shutdownRequested, CountDownLatch stopped, Config config) {
      this.injector = requireNonNull(injector);
      this.shutdownRequested = requireNonNull(shutdownRequested);
      this.stopped = requireNonNull(stopped);
      this.config = requireNonNull(config);
    }

    @Override
    public Service getService() {
      return ServiceImpl.this;
    }

    @Override
    public Config getConfig() {
      return config;
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
      return injector.getInstance(CoreModule.ARGS);
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
      return shutdownRequested.getCount() == 0L;
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

  private static class CoreModule extends AbstractModule {

    static final Key<ImmutableList<String>> ARGS =
        new Key<ImmutableList<String>>(Names.named(Services.INJECT_ARGS)) {
        };
    static final Key<Map<String, String>> ENVIRONMENT =
        new Key<Map<String, String>>(Names.named(Services.INJECT_ENVIRONMENT)) {
        };

    static final Key<String> SERVICE_NAME =
        new Key<String>(Names.named(Services.INJECT_SERVICE_NAME)) {
        };

    private final ServiceImpl service;
    private final Signaller signaller;
    private final Closer closer;
    private final ImmutableList<String> unprocessedArgs;
    private final Map<String, String> env;
    private final Config config;

    CoreModule(
        ServiceImpl service, Signaller signaller,
        Closer closer, ImmutableList<String> unprocessedArgs, Map<String, String> env,
        Config config) {
      this.service = service;
      this.signaller = signaller;
      this.closer = closer;
      this.unprocessedArgs = unprocessedArgs;
      this.env = env;
      this.config = requireNonNull(config);
    }

    @Override
    protected void configure() {
      // Guice will happily construct objects of concrete classes with a default constructor to
      // satisfy such injections. This option will disable the implicit bindings and require them
      // to be explicitly bound. Since this module is always loaded, we'll always require explicit
      // bindings for all other module.
      binder().requireExplicitBindings();

      bind(Service.class).toInstance(service);
      bind(Signaller.class).toInstance(signaller);
      bind(Closer.class).toInstance(closer);
      bind(SERVICE_NAME).toInstance(service.getServiceName());
      bind(ARGS).toInstance(unprocessedArgs);
      bind(ENVIRONMENT).toInstance(env);
      bind(Config.class).toInstance(config);
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("id", "apollo")
          .toString();
    }
  }

  private static class SignallerImpl implements Signaller {

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
