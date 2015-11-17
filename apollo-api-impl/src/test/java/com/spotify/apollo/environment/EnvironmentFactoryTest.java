/*
 * -\-\-
 * Spotify Apollo API Implementations
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

import com.google.common.collect.Iterables;
import com.google.common.io.Closer;

import com.spotify.apollo.Client;
import com.spotify.apollo.Environment;
import com.spotify.apollo.Response;
import com.spotify.apollo.route.AsyncHandler;
import com.spotify.apollo.route.Route;
import com.spotify.apollo.route.SyncHandler;
import com.typesafe.config.Config;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import okio.ByteString;

import static com.spotify.apollo.environment.EnvironmentFactory.RoutingContext;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

/**
 * Unit-tests for EnvironmentImpl class.
 */
@RunWith(MockitoJUnitRunner.class)
public class EnvironmentFactoryTest {

  private static final String BACKEND_DOMAIN = "dummydomain";
  private static final String SERVICE_NAME = "dummy-service";

  @Mock Client client;
  @Mock EnvironmentConfigResolver configResolver;
  @Mock EnvironmentFactory.Resolver resolver;
  @Mock RoutingContext routingContext;
  @Mock Config configNode;
  @Mock ClassLoader classLoader;

  Closer closer = Closer.create();

  EnvironmentFactoryBuilder sut;

  @Before
  public void setUp() throws Exception {
    sut = EnvironmentFactoryBuilder.newBuilder(BACKEND_DOMAIN, client, closer, resolver);
  }

  @Test
  public void shouldResolveThroughResolver() throws Exception {
    when(resolver.resolve(String.class)).thenReturn("hello world");
    final Environment environment = sut.build().create(SERVICE_NAME, routingContext);

    final String resolve = environment.resolve(String.class);
    assertEquals("hello world", resolve);
  }

  @Test
  public void verifyDummyConfig() {
    final Environment environment = sut.build().create(SERVICE_NAME, routingContext);
    final Config config = environment.config();

    assertNotNull(config);
    assertEquals("propertyBiValue", config.getString("propertyBi"));
  }

  @Test
  public void customConfigResolverShouldWork() throws Exception {
    when(configResolver.getConfig(SERVICE_NAME)).thenReturn(configNode);

    final Environment environment =
        sut.withConfigResolver(configResolver).build()
            .create(SERVICE_NAME, routingContext);

    assertEquals(configNode, environment.config());
  }

  @Test
  public void staticConfigShouldWork() throws Exception {
    final Environment environment =
        sut.withStaticConfig(configNode).build()
            .create(SERVICE_NAME, routingContext);

    assertEquals(configNode, environment.config());
  }

  @Test
  public void shouldCollectRegisteredRoutes() throws Exception {
    final EnvironmentFactory factory = sut.build();

    final RoutingContext routingContext = factory.createRoutingContext();
    final Environment environment = factory.create(SERVICE_NAME, routingContext);

    final Route<AsyncHandler<Response<ByteString>>> route1 =
        Route.sync("GET", "/f1", handler());
    final Route<AsyncHandler<Response<ByteString>>> route2 =
        Route.sync("GET", "/2", handler());
    final Route<AsyncHandler<Response<ByteString>>> route3 =
        Route.sync("GET", "/3", handler());

    environment.routingEngine()
        .registerSafeRoute(route1)
        .registerSafeRoute(route2);

    environment.routingEngine()
        .registerSafeRoute(route3);

    final Iterable<Object> objects = routingContext.endpointObjects();
    assertTrue(Iterables.contains(objects, route1));
    assertTrue(Iterables.contains(objects, route2));
    assertTrue(Iterables.contains(objects, route3));
  }

  @Test
  public void shouldThrowIfUsedAfterInit() throws Exception {
    final EnvironmentFactory factory = sut.build();

    final RoutingContext routingContext = factory.createRoutingContext();
    final Environment environment = factory.create(SERVICE_NAME, routingContext);

    final Route<AsyncHandler<Response<ByteString>>> route =
        Route.sync("GET", "/f1", handler());

    environment.routingEngine()
        .registerSafeRoute(route);

    final Iterable<Object> objects = routingContext.endpointObjects();
    assertTrue(Iterables.contains(objects, route));

    try {
      environment.routingEngine().registerSafeRoute(route);
      fail("should throw");
    } catch (Exception e) {
      assertThat(e.getMessage(), containsString("already been initialized"));
    }
  }

  @Test(expected = IllegalStateException.class)
  public void settingMultipleConfigResolversShouldFail1() throws Exception {
    sut
        .withConfigResolver(configResolver)
        .withStaticConfig(configNode)
        .build();
  }

  @Test(expected = IllegalStateException.class)
  public void settingMultipleConfigResolversShouldFail2() throws Exception {
    sut
        .withStaticConfig(configNode)
        .withConfigResolver(configResolver)
        .build();
  }

  @Test(expected = IllegalStateException.class)
  public void settingMultipleConfigResolversShouldFail3() throws Exception {
    sut
        .withStaticConfig(configNode)
        .withClassLoader(classLoader)
        .build();
  }

  @Test(expected = IllegalStateException.class)
  public void settingMultipleConfigResolversShouldFail4() throws Exception {
    sut
        .withClassLoader(classLoader)
        .withStaticConfig(configNode)
        .build();
  }

  @Test(expected = IllegalStateException.class)
  public void settingMultipleConfigResolversShouldFail5() throws Exception {
    sut
        .withConfigResolver(configResolver)
        .withClassLoader(classLoader)
        .build();
  }

  @Test(expected = IllegalStateException.class)
  public void settingMultipleConfigResolversShouldFail6() throws Exception {
    sut
        .withClassLoader(classLoader)
        .withConfigResolver(configResolver)
        .build();
  }

  private SyncHandler<Response<ByteString>> handler() {
    return ctx -> Response.ok();
  }
}
