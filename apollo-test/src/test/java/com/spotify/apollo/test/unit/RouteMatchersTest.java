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
package com.spotify.apollo.test.unit;

import com.spotify.apollo.route.AsyncHandler;
import com.spotify.apollo.route.Route;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.StringDescription;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RouteMatchersTest {

  private static final Route<AsyncHandler<String>> TEST_ROUTE = Route.sync("GET", "/path", (rc) -> "yo");

  private Description description = new StringDescription();

  @Before
  public void setUp() throws Exception {

  }

  @Test
  public void hasUriAndMethod_shouldMatchMethodAndPath() {
    Matcher<Route> matcher = RouteMatchers.hasUriAndMethod("GET", "/path");

    boolean result = matcher.matches(TEST_ROUTE);
    assertTrue(result);
  }

  @Test
  public void hasUriAndMethod_shouldFailMethod() {
    boolean result = RouteMatchers.hasUriAndMethod("PUT", "/path").matches(TEST_ROUTE);
    assertFalse(result);
  }

  @Test
  public void hasUriAndMethod_shouldFailPath() {
    boolean result = RouteMatchers.hasUriAndMethod("GET", "/foo").matches(TEST_ROUTE);
    assertFalse(result);
  }

  @Test
  public void hasUriAndMethod_shouldFailMethodAndPath() {
    boolean result = RouteMatchers.hasUriAndMethod("PUT", "/foo").matches(TEST_ROUTE);
    assertFalse(result);
  }

  @Test
  public void hasUri_shouldMatchPath() {
    boolean result = RouteMatchers.hasUri("/path").matches(TEST_ROUTE);
    assertTrue(result);
  }

  @Test
  public void hasUri_shouldFail() {
    boolean result = RouteMatchers.hasUri("/foo").matches(TEST_ROUTE);
    assertFalse(result);
  }

  @Test
  public void hasMethod_shouldMatchPath() {
    boolean result = RouteMatchers.hasMethod("GET").matches(TEST_ROUTE);
    assertTrue(result);
  }

  @Test
  public void hasMethod_shouldFail() {
    boolean result = RouteMatchers.hasUri("PUT").matches(TEST_ROUTE);
    assertFalse(result);
  }
}
