package com.spotify.apollo.core;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Closer;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;

import com.spotify.apollo.module.ApolloModule;
import com.typesafe.config.Config;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;

/**
 * A service that is controlled by Apollo.
 */
public interface Service {

  /**
   * Returns the name of this service.
   *
   * @return the name of this service.
   */
  String getServiceName();

  /**
   * Starts a new instance of this service that is fully initialized.
   *
   * @param args Command-line arguments for the service.
   * @return a new instance of this service that is up and running.
   * @throws ApolloHelpException     if the user wants to show command-line help and not start the
   *                               application.
   * @throws ApolloCompatException if Apollo compatibility mode was requested but the command line
   *                               was not in the Apollo format.
   * @throws ApolloCliException      if something else related to CLI parsing failed.
   * @throws java.io.IOException   if the application could not start for some other reason.
   */
  Instance start(String... args) throws IOException;

  /**
   * Starts a new instance of this service that is fully initialized. It will pick up the
   * configuration from the *.conf file but can override keys using {@code env}.
   *
   * @param args Command-line arguments for the service.
   * @param env  Environment variables for the service.  These are not additional environment
   *             variables, but instead replaces the set of environment variables that Apollo sees,
   *             generally used for testing.
   * @return a new instance of this service that is up and running.
   * @throws ApolloHelpException     if the user wants to show command-line help and not start the
   *                               application.
   * @throws ApolloCompatException if Apollo compatibility mode was requested but the command line
   *                               was not in the Apollo format.
   * @throws ApolloCliException      if something else related to CLI parsing failed.
   * @throws java.io.IOException   if the application could not start for some other reason.
   */
  @VisibleForTesting
  Instance start(String[] args, Map<String, String> env) throws IOException;

  /**
   * Starts a new instance of this service that is fully initialized. It will initialize the
   * service using the {@code config} passed as an argument and the environment variables.
   *
   * @param args   Command-line arguments for the service.
   * @param config Configuration for the service.
   * @return a new instance of this service that is up and running.
   * @throws ApolloHelpException     if the user wants to show command-line help and not start the
   *                               application.
   * @throws ApolloCompatException if Apollo compatibility mode was requested but the command line
   *                               was not in the Apollo format.
   * @throws ApolloCliException      if something else related to CLI parsing failed.
   * @throws java.io.IOException   if the application could not start for some other reason.
   */
  @VisibleForTesting
  Instance start(String[] args, Config config) throws IOException;

  /**
   * A builder for a new service.
   */
  interface Builder {

    /**
     * Registers the specified module as loadable by this service.  This does not guarantee that
     * the module is actually loaded; the modules themselves will inspect the configuration and
     * actually determine if they should be loaded.
     *
     * @param module The module to register.
     * @return This builder.
     */
    Builder withModule(ApolloModule module);

    /**
     * Enables or disables module discovery, which will use SPI to discover all available modules
     * on the classpath.  By default, module discovery is disabled.
     *
     * @param moduleDiscovery {@code true} if module discovery should be used, {@code false}
     *                        otherwise.
     * @return This builder.
     */
    Builder usingModuleDiscovery(boolean moduleDiscovery);

    /**
     * <p>Enable/disable Apollo Compatibility Mode®.</p>
     *
     * <p>Adds some extra command line argument preprocessing to support apollo-standalone. This
     * means that the two first command line arguments will be removed and put in the config under
     * the 'apollo' key.</p>
     *
     * @param enabled {@code true} to enable Apollo Compatibility Mode®, {@code false} otherwise.
     * @return This builder.
     */
    Builder apolloCompatibilityMode(boolean enabled);

    /**
     * Enable/disable whether the thread calling {@link Service#start(String...)} will be
     * interrupted when the application is requested to shut down.  The default is to not interrupt
     * the thread.
     *
     * @param enabled {@code true} if {@link Thread#interrupt()} should be called on the thread
     *                that called {@link #start(String...)} when the service is signalled to shut
     *                down;
     *                {@code false} if nothing should happen.
     * @return This builder.
     */
    Builder withShutdownInterrupt(boolean enabled);

    /**
     * Enable/disable whether Apollo should handle the {@code --help/-h} flags and display
     * command-line help.  The default is to handle the flags.
     *
     * @param enabled {@code true} if Apollo should intercept {@code --help/-h}, {@code false}
     *                otherwise.
     * @return This builder.
     */
    Builder withCliHelp(boolean enabled);

    /**
     * The Java runtime to use when constructing service instances.  This is only respected by
     * Apollo itself; the service instance(s) configured by Apollo might decide to use the global
     * runtime. The default is to use the global JVM runtime.
     *
     * @param runtime The Java runtime to use when constructing service instances.
     * @return This builder.
     */
    @VisibleForTesting
    Builder withRuntime(Runtime runtime);

    /**
     * Creates a new service based off of this builder.
     *
     * @return The newly created service.
     */
    Service build();
  }

  /**
   * A running service instance.
   */
  interface Instance extends Closeable {

    /**
     * Returns the service that this is an instance of.
     *
     * @return The service that this is an instance of.
     */
    Service getService();

    /**
     * Returns the configuration for this service instance.
     *
     * @return The configuration for this service instance.
     */
    Config getConfig();

    /**
     * Returns a shared {@link com.google.common.util.concurrent.ListeningExecutorService} that has
     * virtually infinite capacity and that can be used for long-running jobs.  This executor is
     * scoped along with the service instance, which avoids the need to use the very problematic
     * daemon threads.
     *
     * @return A shared {@link com.google.common.util.concurrent.ListeningExecutorService}.
     */
    ListeningExecutorService getExecutorService();

    /**
     * Returns a shared {@link com.google.common.util.concurrent.ListeningScheduledExecutorService}
     * that has capacity appropriate for scheduled jobs, i.e. jobs that run periodically and have a
     * limited execution time.  Use {@link #getExecutorService()} for jobs that run forever.  This
     * executor is scoped along with the service instance, which avoids the need to use the very
     * problematic daemon threads.
     *
     * @return A shared {@link com.google.common.util.concurrent.ListeningScheduledExecutorService}.
     */
    ListeningScheduledExecutorService getScheduledExecutorService();

    /**
     * Returns a {@link com.google.common.io.Closer} for convenience, where you can register {@link
     * java.io.Closeable}s that should be closed when the application exits.
     *
     * @return A {@link com.google.common.io.Closer} for convenience.
     */
    Closer getCloser();

    /**
     * Returns the list of command-line arguments that were not recognized by Apollo, in order.
     *
     * @return The list of command-line arguments that were not recognized by Apollo, in order.
     */
    ImmutableList<String> getUnprocessedArgs();

    /**
     * Returns a signaller that can be used to send signals to this service instance.  It can be
     * used to send signals to a running service instance, but after the service instance has
     * exited, sending signals will be no-ops.
     *
     * @return a signaller that can be used to send signals to this service instance.
     */
    Signaller getSignaller();

    /**
     * Get an instance provided by one of the Apollo modules.
     *
     * @param type the type of the instance to get.
     * @return an instance of this type.
     * @throws ApolloConfigurationException if no suitable instance of this type
     *                                                          can be found.
     */
    <T> T resolve(Class<T> type);

    /**
     * A method that will block until the service has stopped.  This will wait until a signal from
     * the environment tells the service to shutdown, or an error occurs.
     */
    void waitForShutdown() throws InterruptedException;

    /**
     * Returns whether the service has gotten a shutdown signal, meaning that {@link
     * #waitForShutdown()} will not block.
     *
     * @return whether the service has gotten a shutdown signal.
     */
    boolean isShutdown();
  }

  /**
   * A way of sending signals to a service instance.
   */
  interface Signaller {

    /**
     * Signals the associated service instance to shut down, if it is still running.
     */
    void signalShutdown();
  }
}
