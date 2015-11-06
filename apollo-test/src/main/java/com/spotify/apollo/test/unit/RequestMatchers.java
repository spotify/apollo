package com.spotify.apollo.test.unit;

import com.spotify.apollo.Request;

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

import static org.hamcrest.CoreMatchers.equalTo;

/**
 * Provides Hamcrest matcher utilities for matching requests. Intended for use with
 * {@link com.spotify.apollo.test.StubClient} response matchers.
 */
public final class RequestMatchers {
  private RequestMatchers() {
    // prevent instantiation
  }

  public static Matcher<Request> uri(Matcher<String> uriMatcher) {
    return new FeatureMatcher<Request, String>(uriMatcher, "uri matches", "uri") {
      @Override
      protected String featureValueOf(Request actual) {
        return actual.uri();
      }
    };
  }

  public static Matcher<Request> method(String method) {
    return new FeatureMatcher<Request, String>(equalTo(method), "method matches", "method") {
      @Override
      protected String featureValueOf(Request actual) {
        return actual.method();
      }
    };
  }
}
