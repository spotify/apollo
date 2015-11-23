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
package com.spotify.apollo.test.helper;

import com.google.common.base.Stopwatch;

import com.spotify.apollo.Environment;
import com.spotify.apollo.Request;
import com.spotify.apollo.Response;
import com.spotify.apollo.module.AbstractApolloModule;
import com.spotify.apollo.test.ServiceHelper;
import com.spotify.apollo.test.StubClient;

import org.junit.AfterClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.concurrent.TimeUnit;

import okio.ByteString;

import static okio.ByteString.encodeUtf8;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ServiceHelperTest {

  private static final Logger LOG = LoggerFactory.getLogger(ServiceHelperTest.class);

  private static final String TEST_THING = "a test string";
  private static final String TEST_CONFIG_THING = "a-test-string-in-the-config";
  private static final String SERVICE_NAME = "test-service";

  @Rule
  public ServiceHelper serviceHelper = ServiceHelper.create(this::appInit, SERVICE_NAME)
      .domain("xyz99.spotify.net")
      .conf("some.key", TEST_CONFIG_THING)
      .args("-v");

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  StubClient stubClient = serviceHelper.stubClient();

  @Mock
  SomeApplication.SomeService someService;

  @Mock
  static SomeApplication.CloseCall closeCall;

  void appInit(Environment environment) {
    SomeApplication someApplication =
        SomeApplication.create(environment, someService, closeCall);

    environment.routingEngine()
        .registerAutoRoutes(someApplication);
  }

  @AfterClass
  public static void tearDown() throws Exception {
    verify(closeCall).didClose();
  }

  @Test
  public void smokeTest() throws Exception {
    when(someService.thatDoesThings()).thenReturn(TEST_THING);

    String response = doGet();
    assertThat(response, is(TEST_THING));
  }

  @Test
  public void smokeTest2() throws Exception {
    when(someService.thatDoesThings()).thenReturn(TEST_THING + " and more");

    String response = doGet();
    assertThat(response, is(TEST_THING + " and more"));
  }

  @Test
  public void testDomain() throws Exception {
    String response = doGet("/domain");
    assertThat(response, is("xyz99.spotify.net"));
  }

  @Test
  public void shouldSeeConfigValues() throws Exception {
    String response = doGet("/conf-key");
    assertThat(response, is(TEST_CONFIG_THING));
  }

  @Test
  public void shouldOverrideConfig() throws Exception {
    serviceHelper = ServiceHelper.create(this::appInit, SERVICE_NAME)
        .conf("some.key", TEST_CONFIG_THING)
        .conf("some.key", "different config")
        .args("-v");
    serviceHelper.start();
    String response = doGet("/conf-key");
    assertThat(response, is("different config"));
  }

  @Test
  public void shouldResetConfig() throws Exception {
    serviceHelper = ServiceHelper.create(this::appInit, SERVICE_NAME)
        .conf("some.key", TEST_CONFIG_THING)
        .resetConf("some.key")
        .args("-v");

    serviceHelper.start();
    String response = doGet("/conf-key");
    assertThat(response, is("no value found for some.key"));
  }

  @Test
  public void callTest() throws Exception {
    stubClient.respond(Response.forPayload(encodeUtf8("hello from something")))
        .in(300, TimeUnit.MILLISECONDS)
        .to("http://something/foo");

    String response = doGet("/call/something/foo");
    assertThat(response, is("hello from something"));
  }

  @Test
  public void testPayload() throws Exception {
    Response<ByteString> thing = serviceHelper.request("POST", "/post", encodeUtf8("bytes"))
        .toCompletableFuture().get();
    assertThat(thing.payload().get().utf8(), is("bytes"));
  }

  @Test
  public void testCallServiceClient() throws Exception {
    stubClient.respond(Response.forPayload(encodeUtf8("hello from something")))
        .to("http://something/foo");

    Request request = Request.forUri("http://" + SERVICE_NAME + "/call/something/foo");
    Response<ByteString> response = serviceHelper.serviceClient().send(request)
        .toCompletableFuture().get();

    assertThat(response.payload().get().utf8(), is("hello from something"));
  }

  @Test
  public void shouldDisallowRequestsBeforeStarted() throws Exception {
    ServiceHelper notStarted = ServiceHelper.create(this::appInit, "test-service")
        .conf("some.key", TEST_CONFIG_THING)
        .args("-v");

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("ServiceHelper not started");

    notStarted.request("GET", "/");
  }

  @Test
  public void shouldTimeoutIfStartupIsSlow() throws Exception {
    ServiceHelper timeout = ServiceHelper.create(ServiceHelperTest::tooSlow, "test-service")
        .conf("some.key", TEST_CONFIG_THING)
        .args("-v")
        .startTimeoutSeconds(1);

    Stopwatch stopwatch = Stopwatch.createStarted();
    try {
      timeout.start();
    }
    catch (RuntimeException e) {
      if (!e.getMessage().contains("a reasonable time")) {
        throw e;
      }
    }
    assertThat(stopwatch.elapsed(TimeUnit.SECONDS), lessThan(3L));
  }

  @Test
  public void shouldCatchExceptionBeforeTimeout() throws Exception {
    ServiceHelper timeout = ServiceHelper.create(ServiceHelperTest::throwsException, "test-service")
        .conf("some.key", TEST_CONFIG_THING)
        .args("-v")
        .startTimeoutSeconds(2);

    Stopwatch stopwatch = Stopwatch.createStarted();

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Service failed during startup");

    timeout.start();

    assertThat(stopwatch.elapsed(TimeUnit.SECONDS), lessThan(1L));
  }

  @Test
  public void shouldCatchErrorBeforeTimeout() throws Exception {
    ServiceHelper timeout = ServiceHelper.create(ServiceHelperTest::throwsError, "test-service")
        .conf("some.key", TEST_CONFIG_THING)
        .args("-v")
        .startTimeoutSeconds(2);

    Stopwatch stopwatch = Stopwatch.createStarted();

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Service failed during startup");

    timeout.start();

    assertThat(stopwatch.elapsed(TimeUnit.SECONDS), lessThan(1L));
  }

  @Test
  public void shouldSupportRegisteringAdditionalModules() throws Exception {
    stubClient.respond(Response.forPayload(encodeUtf8("hello from something")))
        .to("http://something/foo");

    ServiceHelper withModule = ServiceHelper.create(environment -> {
      assertThat(environment.resolve(TestModule.class), is(notNullValue()));
    }, SERVICE_NAME)
        .domain("xyz99.spotify.net")
        .conf("some.key", TEST_CONFIG_THING)
        .withModule(new TestModule())
        .args("-v");

    withModule.start();
  }

    @Test
  public void shouldUseScheme() throws Exception {
    ServiceHelper scheme = ServiceHelper.create(this::appInit, "test-service")
        .scheme("gopher+my-go.pher");

    scheme.start();

    Response<ByteString> response = scheme.request("GET", "/uri")
        .toCompletableFuture().get();

    scheme.close();

    assertThat(response.payload().get().utf8(), is("gopher+my-go.pher://test-service/uri"));
  }

  @Test
  public void shouldValidateScheme() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Illegal scheme format");

    ServiceHelper.create(this::appInit, "test-service").scheme("http://"); // invalid
  }

  private static void tooSlow(Environment environment) {
    try {
      Thread.sleep(15000);
    } catch (InterruptedException e) {
      LOG.info("interrupted because I was too slow, terminating!");
    }
  }

  private static void throwsException(Environment environment) {
    throw new RuntimeException("I fail!");
  }

  private static void throwsError(Environment environment) {
    throw new NoClassDefFoundError("I fail!");
  }

  private String doGet() throws Exception {
    return doGet("/");
  }

  private String doGet(String url) throws Exception {
    final URI uri = URI.create(url);
    final Response<ByteString> response =
        serviceHelper.request("GET", uri).toCompletableFuture().get();

    return response.payload().get().utf8();
  }

  private static class TestModule extends AbstractApolloModule {

    @Override
    protected void configure() {
      bind(TestModule.class).toInstance(this);
    }

    @Override
    public String getId() {
      return "addheaderdecorator";
    }
  }
}
