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

import com.spotify.apollo.AppInit;
import com.spotify.apollo.Environment;
import com.spotify.apollo.RequestContext;
import com.spotify.apollo.module.ApolloModule;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * A JUnit 5 extension for running tests against an apollo service. It is built around the {@link
 * AppInit} setup mechanism and can be used to start a service configured in a way appropriate for
 * the test scenario.
 *
 * <p>Typical usage would use {@link #create(AppInit, String)} together with a {@link
 * RegisterExtension} annotation. Further configuration like config key overrides, running domain
 * and additional program arguments can be set up using {@link #conf(String, String)}, {@link
 * #domain(String)} and {@link #args(String...)} respectively. The created {@link ServiceHelper}
 * could be accessed from a test method using a getter method {@link #getServiceHelper()}. This
 * extension also implements {@link ParameterResolver} and can inject {@link ServiceHelper}
 * instances into test methods via parameter injection.
 *
 * <p><b>Declarative extension registration via {@link ExtendWith} is not supported.</b> An
 * exception will be thrown if this extension is registered declaratively. Only programmatic
 * extension registration via {@link RegisterExtension} is supported.
 *
 * <p>Requests can be sent to the running application using any of {@link ServiceHelper#request}
 * methods.
 *
 * <h2>Example usage for testing a route provider</h2>
 *
 * <pre><code>
 * class MyServiceTest {
 *
 *  {@literal @}RegisterExtension
 *   static final ServiceHelperExtension serviceHelperExtension =
 *       ServiceHelperExtension.create(MyServiceTest::appInit, "my-service")
 *                             .conf("some.key", "some-value")
 *                             .args("-v")
 *                             .startTimeoutSeconds(30);
 *
 *   static void appInit(Environment environment) {
 *     SomeObject someObject = new SomeObject();
 *     // Implements resource "/endpoint" using someObject
 *     RouteProvider endpointResource = new EndpointResource(someObject);
 *     environment.routingEngine()
 *                .registerAutoRoutes(endpointResource);
 *   }
 *
 *  {@literal @}Test
 *   void testRequest() throws Exception {
 *     // access the {@link ServiceHelper} via a getter
 *     // see the next example for parameter injection
 *     ServiceHelper serviceHelper = serviceHelperExtension.getServiceHelper();
 *
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
 *
 * <pre><code>
 * class MyServiceTest {
 *
 *   // Implements {@link AppInit}
 *   static final MyService myService = new MyService();
 *
 *  {@literal @}RegisterExtension
 *   static final ServiceHelperExtension serviceHelperExtension =
 *       ServiceHelperExtension.create(myService, "my-service")
 *                             .conf("some.key", "some-value")
 *                             .args("-v")
 *                             .startTimeoutSeconds(30);
 *
 *  {@literal @}Test
 *   void testRequest(ServiceHelper serviceHelper) throws Exception {
 *     String response = Futures.getUnchecked(serviceHelper.request("GET", "/ping"))
 *         .getPayloads().get(0).toStringUtf8();
 *
 *     assertThat(response, is("pong"));
 *   }
 * }
 * </code></pre>
 *
 * <h2>Faking outgoing request responses</h2>
 *
 * <p>The created service helper instance will contain a {@link StubClient} that can be accessed
 * through {@link ServiceHelper#stubClient()}. This can be used to setup mocked replies on outgoing
 * requests. Requests made by the application will first try to match against requests set up in the
 * {@link StubClient}. But if none is found the request will be delegated to the underlying client
 * that is normally available to the application through {@link Environment#client()} or {@link
 * RequestContext#requestScopedClient()}.
 *
 * <p>See {@link StubClient} for more docs on how to set up mocked request replies.
 *
 * @see ServiceHelper
 * @see RegisterExtension
 * @see ParameterResolver
 */
public final class ServiceHelperExtension
    implements ServerHelperSetup<ServiceHelperExtension>,
    BeforeEachCallback,
    AfterEachCallback,
    ParameterResolver {

  private final ServiceHelper serviceHelper;

  private ServiceHelperExtension() {
    throw new UnsupportedOperationException(
        getClass().getSimpleName() + " does not support declarative registration via @"
            + ExtendWith.class.getSimpleName()
            + ". Please declare this extension programmatically as a field using @"
            + RegisterExtension.class.getSimpleName());
  }

  private ServiceHelperExtension(ServiceHelper serviceHelper) {
    this.serviceHelper = serviceHelper;
  }

  public static ServiceHelperExtension create(AppInit appInit, String serviceName) {
    return new ServiceHelperExtension(ServiceHelper.create(appInit, serviceName));
  }

  public static ServiceHelperExtension create(
      AppInit appInit, String serviceName, StubClient stubClient) {
    return new ServiceHelperExtension(ServiceHelper.create(appInit, serviceName, stubClient));
  }

  @Override
  public ServiceHelperExtension domain(String domain) {
    serviceHelper.domain(domain);
    return this;
  }

  @Override
  public ServiceHelperExtension disableMetaApi() {
    serviceHelper.disableMetaApi();
    return this;
  }

  @Override
  public ServiceHelperExtension args(String... args) {
    serviceHelper.args();
    return this;
  }

  @Override
  public ServiceHelperExtension conf(String key, String value) {
    serviceHelper.conf(key, value);
    return this;
  }

  @Override
  public ServiceHelperExtension conf(String key, Object value) {
    serviceHelper.conf(key, key);
    return this;
  }

  @Override
  public ServiceHelperExtension resetConf(String key) {
    serviceHelper.resetConf(key);
    return this;
  }

  @Override
  public ServiceHelperExtension forwardingNonStubbedRequests(boolean forward) {
    serviceHelper.forwardingNonStubbedRequests(forward);
    return this;
  }

  @Override
  public ServiceHelperExtension startTimeoutSeconds(int timeoutSeconds) {
    serviceHelper.startTimeoutSeconds(timeoutSeconds);
    return this;
  }

  @Override
  public ServiceHelperExtension withModule(ApolloModule module) {
    serviceHelper.withModule(module);
    return this;
  }

  @Override
  public ServiceHelperExtension scheme(String scheme) {
    serviceHelper.scheme(scheme);
    return this;
  }

  @Override
  public void beforeEach(ExtensionContext context) throws Exception {
    serviceHelper.start();
  }

  @Override
  public void afterEach(ExtensionContext context) throws Exception {
    serviceHelper.close();
  }

  @Override
  public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext context)
      throws ParameterResolutionException {
    return ServiceHelper.class.equals(parameterContext.getParameter().getType());
  }

  @Override
  public Object resolveParameter(ParameterContext parameterContext, ExtensionContext context)
      throws ParameterResolutionException {
    return serviceHelper;
  }

  public ServiceHelper getServiceHelper() {
    return serviceHelper;
  }
}
