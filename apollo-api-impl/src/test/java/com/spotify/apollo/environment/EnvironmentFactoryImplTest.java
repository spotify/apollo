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

@RunWith(MockitoJUnitRunner.class)
public class EnvironmentFactoryImplTest {

  private static final String BACKEND_DOMAIN = "dummydomain";
  private static final String SERVICE_NAME = "dummy-service";

  @Mock Client client;
  @Mock EnvironmentConfigResolver configResolver;
  @Mock EnvironmentFactory.Resolver resolver;
  @Mock RoutingContext routingContext;
  @Mock ClassLoader classLoader;

  Closer closer = Closer.create();

  EnvironmentFactoryImpl sut;

  @Before
  public void setUp() throws Exception {
    sut = new EnvironmentFactoryImpl(BACKEND_DOMAIN, client, resolver, closer);
  }

  @Test
  public void shouldResolveThroughResolver() throws Exception {
    when(resolver.resolve(String.class)).thenReturn("hello world");
    final Environment environment = sut.create(SERVICE_NAME, routingContext);

    final String resolve = environment.resolve(String.class);
    assertEquals("hello world", resolve);
  }

  @Test
  public void shouldCollectRegisteredRoutes() throws Exception {

    final RoutingContext routingContext = sut.createRoutingContext();
    final Environment environment = sut.create(SERVICE_NAME, routingContext);

    final Route<AsyncHandler<Response<ByteString>>> route1 =
        Route.sync("GET", "/f1", handler());
    final Route<AsyncHandler<Response<ByteString>>> route2 =
        Route.sync("GET", "/2", handler());
    final Route<AsyncHandler<Response<ByteString>>> route3 =
        Route.sync("GET", "/3", handler());

    environment.routingEngine()
        .registerRoute(route1)
        .registerRoute(route2);

    environment.routingEngine()
        .registerRoute(route3);

    final Iterable<Object> objects = routingContext.endpointObjects();
    assertTrue(Iterables.contains(objects, route1));
    assertTrue(Iterables.contains(objects, route2));
    assertTrue(Iterables.contains(objects, route3));
  }

  @Test
  public void shouldThrowIfUsedAfterInit() throws Exception {

    final RoutingContext routingContext = sut.createRoutingContext();
    final Environment environment = sut.create(SERVICE_NAME, routingContext);

    final Route<AsyncHandler<Response<ByteString>>> route =
        Route.sync("GET", "/f1", handler());

    environment.routingEngine()
        .registerRoute(route);

    final Iterable<Object> objects = routingContext.endpointObjects();
    assertTrue(Iterables.contains(objects, route));

    try {
      environment.routingEngine().registerRoute(route);
      fail("should throw");
    } catch (Exception e) {
      assertThat(e.getMessage(), containsString("already been initialized"));
    }
  }

  private SyncHandler<Response<ByteString>> handler() {
    return ctx -> Response.ok();
  }
}
