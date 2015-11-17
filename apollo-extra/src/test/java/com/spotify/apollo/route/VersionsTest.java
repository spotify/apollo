/*
 * -\-\-
 * Spotify Apollo Extra
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
package com.spotify.apollo.route;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import com.spotify.apollo.Response;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.stream.Stream;

import okio.ByteString;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class VersionsTest {
  Versions versions;

  Route<AsyncHandler<Response<ByteString>>> route;
  Route<AsyncHandler<Response<ByteString>>> route2;
  VersionedRoute versionedRoute;
  VersionedRoute fromVersion2;
  VersionedRoute removedIn2;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Before
  public void setUp() throws Exception {
    route = route("GET", "/foo");
    route2 = route("GET", "/bar");

    versionedRoute = VersionedRoute.of(route);
    fromVersion2 = VersionedRoute.of(route).validFrom(2);
    removedIn2 = VersionedRoute.of(route).removedIn(2);
  }

  private Route<AsyncHandler<Response<ByteString>>> route(String method, String uri) {
    return Route.sync(method, uri, requestContext ->
        Response.forPayload(ByteString.encodeUtf8("hi")));
  }

  @Test
  public void shouldPrependVersionToUriOfRoute() throws Exception {
    versions = Versions.from(0).to(0);

    assertThat(versions.expand(Stream.of(versionedRoute)).map(Route::uri).collect(toList()),
               equalTo(ImmutableList.of("/v0/foo")));
  }

  @Test
  public void shouldStartWithProvidedFirstVersion() throws Exception {
    versions = Versions.from(0).to(2);

    assertThat(versions.expand(Stream.of(fromVersion2)).map(Route::uri).collect(toList()),
               equalTo(ImmutableList.of("/v2/foo")));
  }

  @Test
  public void shouldCreateOneRouteForEachVersionExcludingTheVersionItIsRemovedIn() throws Exception {
    versions = Versions.from(0).to(2);

    assertThat(versions.expand(Stream.of(removedIn2)).map(Route::uri).collect(toSet()),
               equalTo(ImmutableSet.of("/v0/foo", "/v1/foo")));
  }

  @Test
  public void shouldHandleComplexExample() throws Exception {
    versions = Versions.from(1).to(5);

    VersionedRoute versionedRoute2 = VersionedRoute.of(route2).validFrom(2).removedIn(6);

    Stream<Route<AsyncHandler<Response<ByteString>>>> expanded =
        versions.expand(Stream.of(removedIn2, versionedRoute2));
    assertThat(expanded.map(Route::uri).collect(toSet()),
               equalTo(ImmutableSet.of("/v1/foo", "/v2/bar", "/v3/bar", "/v4/bar", "/v5/bar")));
  }

  @Test
  public void shouldFailForOverlappingVersions() throws Exception {
    versions = Versions.from(1).to(1);

    Stream<VersionedRoute> versionedRoutes = Stream.of(versionedRoute, removedIn2);

    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("versioned routes overlap");
    thrown.expectMessage("/v1/foo");

    versions.expand(versionedRoutes);
  }

  @Test
  public void shouldAllowOverlappingUrisForDifferentMethods() {
    versions = Versions.from(1).to(1);

    Stream<VersionedRoute> versionedRoutes = Stream.of(
        VersionedRoute.of(route("GET", "/foo")),
        VersionedRoute.of(route("PUT", "/foo")));

    assertThat(versions.expand(versionedRoutes).map(Versions::methodUri).collect(toSet()),
               equalTo(ImmutableSet.of("GET /v1/foo", "PUT /v1/foo")));
  }
}
