/*
 * -\-\-
 * Spotify Apollo Testing Helpers
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
package com.spotify.apollo.test;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Provides;

import com.spotify.apollo.AppInit;
import com.spotify.apollo.Client;
import com.spotify.apollo.Environment;
import com.spotify.apollo.Request;
import com.spotify.apollo.RequestContext;
import com.spotify.apollo.Response;
import com.spotify.apollo.core.Service;
import com.spotify.apollo.core.Services;
import com.spotify.apollo.environment.ApolloConfig;
import com.spotify.apollo.environment.ApolloEnvironmentModule;
import com.spotify.apollo.http.client.HttpClientModule;
import com.spotify.apollo.meta.MetaModule;
import com.spotify.apollo.metrics.MetricsModule;
import com.spotify.apollo.module.AbstractApolloModule;
import com.spotify.apollo.module.ApolloModule;
import com.spotify.apollo.request.RequestHandler;
import com.spotify.metrics.core.MetricId;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import javax.inject.Singleton;

import okio.ByteString;

import static com.google.common.base.Preconditions.checkState;
import static com.spotify.apollo.environment.ClientDecoratorOrder.beginWith;
import static com.spotify.apollo.http.client.HttpClientModule.HTTP_CLIENT;
import static com.spotify.apollo.meta.MetaModule.OUTGOING_CALLS;
import static com.spotify.apollo.test.ForwardingStubClientModule.STUB_CLIENT;

/**
 * <p>A JUnit {@link TestRule} for running tests against an apollo service. It is built around
 * the {@link AppInit} setup mechanism and can be used to start a service configured in a way
 * appropriate for the test scenario.</p>
 *
 * <p>Typical usage would use {@link #create(AppInit, String)} together with a
 * {@link Rule} annotation. Further configuration like config key overrides, running domain and
 * additional program arguments can be set up using {@link #conf(String, String)},
 * {@link #domain(String)} and {@link #args(String...)} respectively.</p>
 *
 * <p>Requests can be sent to the running application using any of {@link #request} methods.</p>
 *
 * <h2>Example usage for testing a route provider</h2>
 * <pre><code>
 * {@literal @}RunWith(MockitoJUnitRunner.class)
 * class MyServiceTest {
 *
 *   {@literal @}Rule
 *   public ServiceHelper serviceHelper = ServiceHelper.create(this::appInit, "my-service")
 *       .conf("some.key", "some-value")
 *       .args("-v")
 *       .startTimeoutSeconds(30);
 *
 *   {@literal @}Mock
 *   SomeObject someObject;
 *
 *   void appInit(Environment environment) {
 *     // Implements resource "/endpoint" using someObject
 *     RouteProvider endpointResource = new EndpointResource(someObject);
 *     environment.routingEngine()
 *         .registerAutoRoutes(endpointResource);
 *   }
 *
 *   {@literal @}Test
 *   public void testRequest() throws Exception {
 *     when(someObject.thatDoesThings()).thenReturn("a test string");
 *
 *     String response = Futures.getUnchecked(serviceHelper.request("GET", "/endpoint"))
 *         .getPayloads().get(0).toStringUtf8();
 *
 *     assertThat(response, is("a test string"));
 *   }
 * }
 * </code></pre>
 *
 * <h2>Example usage for system or acceptance tests</h2>
 * <pre><code>
 * {@literal @}RunWith(MockitoJUnitRunner.class)
 * class MyServiceTest {
 *
 *   // Implements {@link AppInit}
 *   MyService myService = new MyService();
 *
 *   {@literal @}Rule
 *   public ServiceHelper serviceHelper = ServiceHelper.create(myService, "my-service")
 *       .conf("some.key", "some-value")
 *       .args("-v")
 *       .startTimeoutSeconds(30);
 *
 *   {@literal @}Test
 *   public void testRequest() throws Exception {
 *     String response = Futures.getUnchecked(serviceHelper.request("GET", "/ping"))
 *         .getPayloads().get(0).toStringUtf8();
 *
 *     assertThat(response, is("pong"));
 *   }
 * }
 * </code></pre>
 *
 * <h1>Faking outgoing request responses</h1>
 *
 * <p>The service helper instance will contain a {@link StubClient} that can be accessed
 * through {@link #stubClient()}. This can be used to setup mocked replies on outgoing requests.
 * Requests made by the application will first try to match against requests set up in the
 * {@link StubClient}. But if none is found the request will be delegated to the underlying
 * client that is normally available to the application through {@link Environment#client()} or
 * {@link RequestContext#requestScopedClient()}.</p>
 *
 * <p>See {@link StubClient} for more docs on how to set up mocked request replies.</p>
 */
public class ServiceHelper implements TestRule, Closeable {

  private static final Logger LOG = LoggerFactory.getLogger(ServiceHelper.class);
  public static final String[] NO_ARGS = new String[0];
  private static final String DEFAULT_SCHEME = "http";

  // https://tools.ietf.org/html/rfc3986#section-3.1 Scheme
  // scheme      = ALPHA *( ALPHA / DIGIT / "+" / "-" / "." )
  private static final Pattern SCHEME_RE = Pattern.compile("[a-zA-Z][a-zA-Z0-9+.-]*");

  private final CountDownLatch started = new CountDownLatch(1);

  private final AppInit appInit;
  private final String serviceName;

  private final StubClient stubClient;
  private final Client serviceClient;
  private final List<ApolloModule> additionalModules;

  private Config conf;
  private final ExecutorService executor = Executors.newSingleThreadExecutor(
      new ThreadFactoryBuilder()
          .setNameFormat("apollo-servicehelper-%d")
          .build()
  );

  private String[] args = NO_ARGS;

  private Future currentHelperFuture;

  private Service.Instance instance;
  private RequestHandler requestHandler;

  private boolean forwardNonStubbedRequests = true;
  private int timeoutSeconds = 5;
  private String scheme = DEFAULT_SCHEME;

  private ServiceHelper(AppInit appInit, String serviceName, StubClient stubClient) {
    this.appInit = appInit;
    this.serviceName = serviceName;
    this.stubClient = Objects.requireNonNull(stubClient);
    this.serviceClient = this::request;
    this.conf = ConfigFactory.load(serviceName);
    additionalModules = new ArrayList<>();
  }

  /**
   * Creates a {@link ServiceHelper} using the given {@link AppInit} and service name.
   *
   * @param appInit      The init function for the test setup
   * @param serviceName  The service name for looking up config
   * @return A ServiceHelper to be used with a JUnit {@link Rule}
   */
  public static ServiceHelper create(AppInit appInit, String serviceName) {
    return new ServiceHelper(appInit, serviceName, new StubClient());
  }


  /**
   * Creates a {@link ServiceHelper} using the given {@link AppInit}, service name and
   * stub client. Use, for instance, when you want to configure the thread count of the stub client.
   *
   * @param appInit      The init function for the test setup
   * @param serviceName  The service name for looking up config
   * @param stubClient   The stub client to use
   * @return A ServiceHelper to be used with a JUnit {@link Rule}
   */
  public static ServiceHelper create(AppInit appInit, String serviceName, StubClient stubClient) {
    return new ServiceHelper(appInit, serviceName, stubClient);
  }

  /**
   * Run the service in the given domain. This will set the {@code "apollo.backend"} config key
   * which is also available through {@link Environment#domain()}.
   *
   * @param domain  The domain to use
   * @return This ServiceHelper instance
   */
  public ServiceHelper domain(String domain) {
    return conf(Services.CommonConfigKeys.APOLLO_DOMAIN.getKey(), domain);
  }

  /**
   * Don't set up {@code /_meta/*} routes for the application.
   *
   * @return This ServiceHelper instance
   */
  public ServiceHelper disableMetaApi() {
    return conf("apollo.metaApi", "false");
  }

  /**
   * Run the service with the given program arguments.
   *
   * @param args  The program arguments to use
   * @return This ServiceHelper instance
   */
  public ServiceHelper args(String... args) {
    this.args = args;
    return this;
  }

  /**
   * Run the service with the key/value pair defined in the loaded configuration. The key/values
   * defined through this method will be overlayed over any existing config loaded through the
   * given service name when creating this ServiceHelper.
   *
   * @param key    The key to define
   * @param value  The value to associate with the key
   * @return This ServiceHelper instance
   */
  public ServiceHelper conf(String key, String value) {
    conf = conf.withValue(
        key,
        ConfigValueFactory.fromAnyRef(value, "Overridden var in ServiceHelper: " + key));
    return this;
  }

  /**
   * Run the service with the key/value pair defined in the loaded configuration. The key/values
   * defined through this method will be overlayed over any existing config loaded through the
   * given service name when creating this ServiceHelper.
   *
   * @param key   The key to define
   * @param value The value for the configuration. It can be any accepted type as described by
   *              this method's documentation:
   *              {@link com.typesafe.config.ConfigValueFactory#fromAnyRef(
   *              java.lang.Object, java.lang.String)}
   * @return      This ServiceHelper instance
   */
  public ServiceHelper conf(String key, Object value) {
    conf = conf.withValue(
        key,
        ConfigValueFactory.fromAnyRef(value, "Overridden var in ServiceHelper: " + key));
    return this;
  }

  /**
   * Reset a key in the configuration
   *
   * @param key The path to unset
   * @return    This ServiceHelper instance
   */
  public ServiceHelper resetConf(String key) {
    conf = conf.withoutPath(key);
    return this;
  }

  /**
   * Determines whether to forward requests for which nothing has been stubbed. The default is
   * true. If false, requests that don't match stubs will fail.
   *
   * @param forward whether to enable forwarding
   */
  public ServiceHelper forwardingNonStubbedRequests(boolean forward) {
    this.forwardNonStubbedRequests = forward;
    return this;
  }

  /**
   * Set the time to wait for the service to start before giving up. The default value is 5.
   */
  public ServiceHelper startTimeoutSeconds(int timeoutSeconds) {
    this.timeoutSeconds = timeoutSeconds;
    return this;
  }

  public ServiceHelper withModule(ApolloModule module) {
    this.additionalModules.add(module);
    return this;
  }

  /**
   * Set the scheme to be used for relative request uris on this ServiceHelper instance.
   *
   * When request() methods are called with a uri without scheme, scheme://serviceName is prepended
   *
   * @param scheme The scheme to be used for relative request uris (without "://")
   * @return      This ServiceHelper instance
   */
  public ServiceHelper scheme(String scheme) {
    Preconditions.checkArgument(
        SCHEME_RE.matcher(scheme).matches(),
        "Illegal scheme format in " + scheme + " (no not include ://)");
    this.scheme = scheme;
    return this;
  }

  /**
   * A {@link StubClient} that can be used to mock outgoing application request responses.
   *
   * @return the stub client for this service helper instance
   */
  public StubClient stubClient() {
    return stubClient;
  }

  /**
   * Get a {@link Client} that allows to make requests to the service created by this helper
   * @return A client that can resolve requests to this service
   */
  public Client serviceClient() {
    return serviceClient;
  }

  /**
   * Make a call to the running application and return a {@link CompletionStage} of the response.
   *
   * @param request  The request to send to the application
   * @return A future of the response
   */
  public CompletionStage<Response<ByteString>> request(Request request) {
    if (started.getCount() != 0) {
      throw new IllegalStateException(
          "ServiceHelper not started. This can be solved setting it up as a JUnit @Rule or calling the start() method.");
    }

    final FakeOngoingRequest ongoingRequest = new FakeOngoingRequest(request);
    requestHandler.handle(ongoingRequest);
    return ongoingRequest.getReply();
  }

  @VisibleForTesting
  String addSchemaAuthForRelative(String uriString) {
    if (uriString.startsWith("/")) { // relative
      return scheme + "://" + serviceName + uriString;
    } else {
      return uriString;
    }
  }

  /**
   * Makes a call on the given uri. The uri can be an application relative path such as
   * {@code "/ping"} or a full path like {@link "http://<service-name>/ping"}.
   *
   * @param method  The method of the call
   * @param uri     The uri of the call
   * @return A future of the response
   */
  public CompletionStage<Response<ByteString>> request(String method, String uri) {
    return request(method, URI.create(uri));
  }

  /**
   * Makes a call on the given uri. The uri can be an application relative path such as
   * {@code "/ping"} or a full path like {@link "http://<service-name>/ping"}.
   *
   * @param method  The method of the call
   * @param uri     The uri of the call
   * @return A future of the response
   */
  public CompletionStage<Response<ByteString>> request(String method, URI uri) {
    final String uriString = addSchemaAuthForRelative(uri.toString());

    return request(Request.forUri(uriString, method));
  }

  /**
   * Makes a call on the given uri. The uri can be an application relative path such as
   * {@code "/ping"} or a full path like {@link "http://<service-name>/ping"}.
   *
   * @param method  The method of the call
   * @param uri     The uri of the call
   * @param payload A payload body
   * @return A future of the response
   */
  public CompletionStage<Response<ByteString>> request(String method, String uri, ByteString payload) {
    return request(method, URI.create(uri), payload);
  }

  /**
   * Makes a call on the given uri. The uri can be an application relative path such as
   * {@code "/ping"} or a full path like {@link "http://<service-name>/ping"}.
   *
   * @param method  The method of the call
   * @param uri     The uri of the call
   * @param payload A payload body
   * @return A future of the response
   */
  public CompletionStage<Response<ByteString>> request(String method, URI uri, ByteString payload) {
    final String uriString = addSchemaAuthForRelative(uri.toString());

    return request(Request.forUri(uriString, method).withPayload(payload));
  }

  @Override
  public Statement apply(final Statement base, final Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        try {
          start(timeoutSeconds, args);
          base.evaluate();
        } catch (Exception e) {
          throw Throwables.propagate(e);
        } finally {
          ServiceHelper.this.close();
        }
      }
    };
  }

  @Override
  public void close() throws IOException {
    try {
      stubClient.clear();
    } catch (Exception e) {
      LOG.warn("Exception when clearing StubClient", e);
    }
    shutdown();
  }

  /**
   * Starts the service, blocking for maximum {@code timeoutSeconds} until it has come up.
   */
  public void start() throws InterruptedException {
    start(timeoutSeconds, args);
  }

  private void start(int timeoutSeconds, final String... args) throws InterruptedException {
    checkState(currentHelperFuture == null, "currentHelperFuture non-null!");

    currentHelperFuture = executor.submit(() -> {
      try {
        Service.Builder serviceBuilder = Services.usingName(serviceName)
            .usingModuleDiscovery(false)
            .withModule(
                ApolloEnvironmentModule.create(beginWith(OUTGOING_CALLS, STUB_CLIENT)
                                                   .endWith(HTTP_CLIENT)))
            .withModule(MetaModule.create("service-helper"))
            .withModule(HttpClientModule.create())
            .withModule(MetricsModule.create())
            .withModule(new MetricIdModule())
            .withModule(
                ForwardingStubClientModule
                    .create(forwardNonStubbedRequests, stubClient.asRequestAwareClient()));

        for (ApolloModule module : additionalModules) {
          serviceBuilder = serviceBuilder.withModule(module);
        }

        final Service service = serviceBuilder.build();

        LOG.info("Starting with args: {}", Arrays.toString(args));

        try (Service.Instance instance = service.start(args, conf)) {
          final RequestHandler envRequestHandler =
              ApolloEnvironmentModule.environment(instance)
                  .initialize(appInit);

          final ApolloConfig config = instance.resolve(ApolloConfig.class);

          LOG.info("Started {} with backend domain '{}'", serviceName, config.backend());

          instanceCreated(instance, envRequestHandler);

          instance.waitForShutdown();

          LOG.info("Shutting down {}", serviceName);
        }
      } catch (Throwable e) {
        LOG.error("Failed to start service", e);
        started.countDown();
      }
    });
    if (!started.await(timeoutSeconds, TimeUnit.SECONDS)) {
      currentHelperFuture.cancel(true);
      currentHelperFuture = null;
      throw new IllegalStateException("Service did not start within a reasonable time");
    }
    if (instance == null) {
      throw new IllegalStateException("Service failed during startup");
    }
  }

  private void instanceCreated(Service.Instance instance, RequestHandler requestHandler) {
    try {
      shutdown();
    } catch (Throwable throwable) {
      LOG.warn("failed to shutdown previous instance", throwable);
    }

    LOG.info("Got instance {}", instance);
    this.instance = instance;
    this.requestHandler = requestHandler;
    started.countDown();
  }

  private void shutdown() {
    if (instance != null) {
      instance.getSignaller().signalShutdown();
      requestHandler = null;
      instance = null;
      Futures.getUnchecked(currentHelperFuture);
      currentHelperFuture = null;
    }
  }

  private static class MetricIdModule extends AbstractApolloModule {
    @Override
    protected void configure() {

    }

    @Override
    public String getId() {
      return "servicehelper-metric-id";
    }

    @Provides
    @Singleton
    public MetricId metricId() {
      return MetricId.build("apollo").tagged(
          "service-framework", "service-helper");
    }
  }
}
