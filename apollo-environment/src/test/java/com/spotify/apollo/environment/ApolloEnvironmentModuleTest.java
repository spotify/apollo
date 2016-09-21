/*
 * -\-\-
 * Spotify Apollo API Environment
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
package com.spotify.apollo.environment;

import com.google.common.collect.ImmutableMap;

import com.spotify.apollo.AppInit;
import com.spotify.apollo.Environment;
import com.spotify.apollo.Request;
import com.spotify.apollo.RequestMetadata;
import com.spotify.apollo.Response;
import com.spotify.apollo.Status;
import com.spotify.apollo.core.Service;
import com.spotify.apollo.core.Services;
import com.spotify.apollo.request.OngoingRequest;
import com.spotify.apollo.request.RequestHandler;
import com.spotify.apollo.request.RequestMetadataImpl;
import com.spotify.apollo.route.Route;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import okio.ByteString;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ApolloEnvironmentModuleTest {

  private static final String[] ZERO_ARGS = new String[0];

  private ApolloEnvironmentModule appModule;
  private Service.Builder service;

  @Before
  public void setUp() throws Exception {
    appModule = ApolloEnvironmentModule.create();
    service = Services.usingName("ping")
        .withModule(appModule);
  }

  @Test
  public void shouldHandleAppInit() throws Exception {
    final AtomicBoolean init = new AtomicBoolean();
    final AtomicBoolean destroy = new AtomicBoolean();

    try (Service.Instance i = service.build().start()) {
      final ApolloEnvironment environment = ApolloEnvironmentModule.environment(i);
      final RequestHandler handler = environment.initialize(
          env -> {
            init.set(true);
            env.closer().register(() -> destroy.set(true));
          });
      assertNotNull(handler);
    } catch (IOException e) {
      fail(e.getMessage());
    }

    assertTrue(init.get());
    assertTrue(destroy.get());
  }

  @Test
  public void shouldGetFunctioningEnvironment() throws Exception {
    final AtomicReference<Environment> envReference = new AtomicReference<>();

    try (Service.Instance i = service.build().start()) {
      final ApolloEnvironment environment = ApolloEnvironmentModule.environment(i);
      final RequestHandler handler = environment.initialize(new EnvApp(envReference::set));
      assertNotNull(handler);

      final Environment e = envReference.get();
      validateEnv(e);
    } catch (IOException e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void shouldGetFunctioningEnvironmentInAppInit() throws Exception {
    final AtomicReference<Environment> envReference = new AtomicReference<>();

    try (Service.Instance i = service.build().start()) {
      final ApolloEnvironment environment = ApolloEnvironmentModule.environment(i);
      final RequestHandler handler = environment.initialize(envReference::set);
      assertNotNull(handler);

      final Environment e = envReference.get();
      validateEnv(e);
    } catch (IOException e) {
      fail(e.getMessage());
    }
  }

  private static void validateEnv(Environment e) {
    assertNotNull(e);
    assertNotNull(e.config());
    assertNotNull(e.client());
    assertNotNull(e.routingEngine());

    assertEquals("", e.domain());
  }

  @Test
  public void shouldSetUpConfig() throws Exception {
    final AtomicReference<Environment> envReference = new AtomicReference<>();

    final Map<String, String> env = ImmutableMap.of("APOLLO_APOLLO_DOMAIN", "my-domain");

    try (Service.Instance i = service.build().start(ZERO_ARGS, env)) {
      final ApolloEnvironment environment = ApolloEnvironmentModule.environment(i);
      final RequestHandler handler = environment.initialize(new EnvApp(envReference::set));
      assertNotNull(handler);

      final Environment e = envReference.get();
      assertNotNull(e);
      assertNotNull(e.config());

      assertEquals("baz", e.config().getString("bar")); // from ping.conf
      assertEquals("my-domain", e.domain());
    } catch (IOException e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void shouldUseRequestRunnableFactoryDecorator() throws Exception {
    final AtomicInteger counter = new AtomicInteger();

    final Service service = this.service
        .withModule(new RequestInspectingModule("http://foo", counter))
        .build();

    try (Service.Instance i = service.start()) {
      final ApolloEnvironment environment = ApolloEnvironmentModule.environment(i);
      final RequestHandler handler = environment.initialize(
          env -> {
          });
      assertNotNull(handler);

      final OngoingRequest ongoingRequest = ongoingRequest("http://foo");

      handler.handle(ongoingRequest);
      assertEquals(1, counter.get());
      handler.handle(ongoingRequest);
      assertEquals(2, counter.get());
    } catch (IOException e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void shouldUseEndpointRunnableFactoryDecorator() throws Exception {
    AtomicReference<String> lastResponseRef = new AtomicReference<>();

    final Service service = this.service
        .withModule(new LastResponseModule(lastResponseRef))
        .build();

    try (Service.Instance i = service.start()) {
      final ApolloEnvironment environment = ApolloEnvironmentModule.environment(i);
      final RequestHandler handler = environment.initialize(
          env -> {
            env.routingEngine()
                .registerAutoRoute(Route.sync("GET", "/", ctx -> "hello"));
          });
      assertNotNull(handler);

      final FakeOngoingRequest ongoingRequest = ongoingRequest("http://foo");

      handler.handle(ongoingRequest);
      assertThat(ongoingRequest.getReply(), hasStatus(Status.GONE));
      assertEquals("hello", lastResponseRef.get());
    } catch (IOException e) {
      fail(e.getMessage());
    }
  }

  Matcher<Response<ByteString>> hasStatus(Status statusCode) {
    return new TypeSafeMatcher<Response<ByteString>>() {
      @Override
      protected boolean matchesSafely(Response<ByteString> item) {
        return item.status() == statusCode;
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("Message with status code ");
        description.appendValue(statusCode);
      }
    };
  }

  private FakeOngoingRequest ongoingRequest(String uri) {
    return new FakeOngoingRequest(Request.forUri(uri));
  }

  private static class EnvApp implements AppInit {

    private final Consumer<Environment> envCallback;

    EnvApp(Consumer<Environment> envCallback) {
      this.envCallback = envCallback;
    }

    @Override
    public void create(Environment environment) {
      assertNotNull(environment);
      envCallback.accept(environment);
    }
  }

  private static class FakeOngoingRequest implements OngoingRequest {

    private final Request request;
    private volatile Response<ByteString> reply = null;

    FakeOngoingRequest(Request request) {
      this.request = request;
    }

    @Override
    public Request request() {
      return request;
    }

    @Override
    public void reply(Response<ByteString> response) {
      reply = response;
    }

    @Override
    public void drop() {
    }

    @Override
    public boolean isExpired() {
      return false;
    }

    @Override
    public RequestMetadata metadata() {
      return RequestMetadataImpl.create(Instant.EPOCH, Optional.empty(), Optional.empty());
    }

    public Response<ByteString> getReply() {
      return reply;
    }
  }
}
