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

import com.spotify.apollo.route.Route;

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.equalTo;

/**
 * A matcher that will compare the method and URI.
 */
public class RouteMatchers {

  /**
   * Build a matcher.
   *
   * @param method The method to match
   * @param uri    The uri to match
   * @return A matcher
   */
  public static Matcher<Route> hasUriAndMethod(final String method, final String uri) {
    return both(new RouteHasMethod(method)).and(new RouteHasUri(uri));
  }

  public static Matcher<Route> hasUri(final String uri) {
    return new RouteHasUri(uri);
  }

  public static Matcher<Route> hasMethod(final String method) {
    return new RouteHasMethod(method);
  }


  /**
   * Matcher for the method part of a {@link Route}.
   */
  private static class RouteHasMethod extends FeatureMatcher<Route, String> {

    public RouteHasMethod(final String method) {
      super(equalTo(method), "method", "method");
    }

    @Override
    protected String featureValueOf(final Route actual) {
      return actual.method();
    }
  }

  /**
   * Matcher for the uri part of a {@link Route}.
   */
  private static class RouteHasUri extends FeatureMatcher<Route, String> {

    /**
     * Constructor.
     *
     * @param uri The uri to match
     */
    public RouteHasUri(final String uri) {
      super(equalTo(uri), "uri", "uri");
    }

    @Override
    protected String featureValueOf(final Route actual) {
      return actual.uri();
    }
  }
}
