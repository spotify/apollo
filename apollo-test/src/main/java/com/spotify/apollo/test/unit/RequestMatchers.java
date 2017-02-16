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

import com.spotify.apollo.test.unit.matchers.request.HasQueryParameterMatcher;
import com.spotify.apollo.test.unit.matchers.request.HeaderMatcher;
import com.spotify.apollo.test.unit.matchers.request.NoHeadersMatcher;
import org.hamcrest.Description;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.hamcrest.core.IsAnything;

import static org.hamcrest.CoreMatchers.anything;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;

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

  /**
   * Matches a {@link Request} that has no query parameters
   * @return A {@link Matcher} matching a request with no query parameters
   */
  public static Matcher<Request> hasNoQueryParameters() {
    return new TypeSafeDiagnosingMatcher<Request>() {
      @Override
      protected boolean matchesSafely(Request item, Description mismatchDescription) {
         if (!item.parameters().isEmpty()) {
           mismatchDescription.appendText("Request had query parameters: ")
               .appendValueList("[", ", ", "]", item.parameters().entrySet());
           return false;
         }
         return true;
      }

      @Override
      public void describeTo(final Description description) {
        description.appendText("Request with no query parameters");
      }
    };
  }

  /**
   * Matches a {@link Request} that has a query parameter with the specified key
   * @param key The query parameter key the matcher will look for
   * @return A {@link Matcher} matching a request with a query parameter for the specified key
   */
  public static Matcher<Request> hasQueryParameter(String key) {
    return new HasQueryParameterMatcher(key, new IsAnything<>());
  }

  /**
   * Matches a {@link Request} that has a query parameter with the specified value
   * @param key The query parameter key
   * @param value The single value of the query parameter
   * @return A {@link Matcher} matching a request with a query parameter with the specified key and value
   */
  public static Matcher<Request> hasQueryParameter(String key, String value) {
    return new HasQueryParameterMatcher(key, contains(value));
  }

  /**
   * Matches a {@link Request} that has a query parameter with the specified values
   * @param key The query parameter key
   * @param matcher The matcher used to match match the query parameter values
   * @return A {@link Matcher} matching a request with a query parameter with the specified key and matching values
   */
  public static Matcher<Request> hasQueryParameter(String key, Matcher<Iterable<? extends String>> matcher) {
    return new HasQueryParameterMatcher(key, matcher);
  }
}
