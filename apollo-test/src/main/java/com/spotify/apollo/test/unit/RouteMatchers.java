/**
 * Copyright (C) 2015 Spotify AB.
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
