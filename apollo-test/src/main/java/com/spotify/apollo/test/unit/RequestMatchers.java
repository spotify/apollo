/*
 * -\-\-
 * Spotify Apollo Testing Helpers
 * --
 * Copyright (C) 2013 - 2016 Spotify AB
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

import com.spotify.apollo.Request;

import com.spotify.apollo.test.unit.matchers.request.HeaderMatcher;
import com.spotify.apollo.test.unit.matchers.request.NoHeadersMatcher;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.hamcrest.core.IsAnything;

import static org.hamcrest.CoreMatchers.any;
import static org.hamcrest.CoreMatchers.anything;
import static org.hamcrest.CoreMatchers.equalTo;

/**
 * Provides Hamcrest matcher utilities for matching requests. Intended for use with
 * {@link com.spotify.apollo.test.StubClient} response matchers.
 */
public final class RequestMatchers {
  private RequestMatchers() {
    // prevent instantiation
  }

  public static Matcher<Request> uri(String uri) {
    return uri(equalTo(uri));
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

  /**
   * A matcher checking the {@link Request#service()} field
   *
   * @param service The service to check for
   * @return A {@link Matcher} checking the {@link Request#service()} equals service
   */
  public static Matcher<Request> service(String service) {
    return service(equalTo(service));
  }

  /**
   * A matcher for the service field in a request
   *
   * @param serviceMatcher A {@link Matcher} for the service
   * @return A matcher for the service field
   */
  public static Matcher<Request> service(Matcher<String> serviceMatcher) {
    return new FeatureMatcher<Request, String>(serviceMatcher, "service matches", "service") {
      @Override
      protected String featureValueOf(Request actual) {
        return actual.service().orElse(null);
      }
    };
  }

  /**
   * A matcher for a {@link Request} with no headers
   * @param key The header's key to look for
   * @param valueMatcher A {@link Matcher} for the value at the key
   * @return The matcher
   */
  public static Matcher<Request> hasNoHeaders() {
    return new NoHeadersMatcher();
  }

  /**
   * A matcher for a {@link Request} with header matching a value
   * @return The matcher
   */
  public static Matcher<Request> hasHeader(String key, Matcher<String> valueMatcher) {
    return new HeaderMatcher(key, valueMatcher);
  }

  /**
   * A matcher for a {@link Request} with header matching a value
   * @param key The header's key
   * @param value The expected value of the header
   * @return The matcher
   */
  public static Matcher<Request> hasHeader(String key, String value) {
    return new HeaderMatcher(key, equalTo(value));
  }

  /**
   * A matcher for a {@link Request} with a header present
   * @param key The
   * @return
   */
  public static Matcher<Request> hasHeader(String key) {
    return new HeaderMatcher(key, new IsAnything<>());
  }
}
