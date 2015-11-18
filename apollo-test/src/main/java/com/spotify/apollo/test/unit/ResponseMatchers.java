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

import com.spotify.apollo.Response;
import com.spotify.apollo.StatusType;

import org.hamcrest.Description;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.util.Optional;

/**
 * Provides Hamcrest matcher utilities for matching {@link Response}.
 */
public final class ResponseMatchers {

  private ResponseMatchers() {
    //Prevent instantiation
  }

  /**
   * Builds a matcher for {@link Response}s with no headers.
   * @return A matcher
   */
  public static <T> Matcher<Response<T>> hasNoHeaders() {
    return new TypeSafeMatcher<Response<T>>() {
      @Override
      protected boolean matchesSafely(Response<T> item) {
        return item.headers().isEmpty();
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("a response without headers");
      }

      @Override
      protected void describeMismatchSafely(Response<T> item, Description mismatchDescription) {
        mismatchDescription.appendText("it contained headers ").appendValue(item.headers());
      }
    };
  }

  /**
   * Builds a matcher for {@link Response}s with matching header.
   * @param header The header to match.
   * @param valueMatcher {@link Matcher} for the corresponding value.
   * @return A matcher
   */
  public static <T> Matcher<Response<T>> hasHeader(String header, Matcher<String> valueMatcher) {
    return new FeatureMatcher<Response<T>, String>(valueMatcher,
                                                   String.format("a response with header \"%s\" matching", header),
                                                   "value") {

      @Override
      protected String featureValueOf(Response<T> actual) {
        return actual.headers().get(header);
      }
    };
  }

  /**
   * Builds a matcher for {@link Response}s without specific header.
   * @param header Name of the unwanted header.
   * @return A matcher
   */
  public static <T> Matcher<Response<T>> doesNotHaveHeader(String header) {
    return new TypeSafeMatcher<Response<T>>() {
      @Override
      protected boolean matchesSafely(Response<T> item) {
        return item.headers().get(header) == null;
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("a response without the header ").appendValue(header);
      }

      @Override
      protected void describeMismatchSafely(Response<T> item, Description mismatchDescription) {
        mismatchDescription.appendText("it contained the header ");
        mismatchDescription.appendValueList("{", ":", "}", header, item.headers().get(header));
      }

    };
  }

  /**
   * Builds a matcher for {@link Response}s with no payload.
   * @return A matcher
   */
  public static <T> Matcher<Response<T>> hasNoPayload() {
    return new TypeSafeMatcher<Response<T>>() {
      @Override
      protected boolean matchesSafely(Response<T> item) {
        return !item.payload().isPresent();
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("a response without payload");
      }

      @Override
      protected void describeMismatchSafely(Response<T> item, Description mismatchDescription) {
        mismatchDescription.appendText("it contained the payload: ").appendValue(item.payload().get());
      }
    };
  }

  /**
   * Builds a matcher for {@link Response}s with matching payload.
   * @param payloadMatcher {@link Matcher} for the payload.
   * @return A matcher
   */
  public static <T> Matcher<Response<T>> hasPayload(Matcher<? super T> payloadMatcher) {
    return new TypeSafeMatcher<Response<T>>() {
      @Override
      protected boolean matchesSafely(Response<T> item) {
        return item.payload()
            .map(payloadMatcher::matches)
            .orElse(false);
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("a response with payload matching ");
        description.appendDescriptionOf(payloadMatcher);
      }

      @Override
      protected void describeMismatchSafely(Response<T> item, Description mismatchDescription) {
        final Optional<T> payload = item.payload();
        if (!payload.isPresent()) {
          mismatchDescription.appendText("there is no payload");
        } else {
          mismatchDescription.appendText("payload ");
          payloadMatcher.describeMismatch(payload.get(), mismatchDescription);
        }
      }
    };
  }

  /**
   * Builds a matcher for {@link Response}s with matching status.
   * @param statusMatcher {@link Matcher} for the status.
   * @return A matcher
   */
  public static <T> Matcher<Response<T>> hasStatus(Matcher<StatusType> statusMatcher) {
    return new FeatureMatcher<Response<T>, StatusType>(statusMatcher,
                                                       "a response with status matching",
                                                       "status") {
      @Override
      protected StatusType featureValueOf(Response<T> item) {
        return item.status();
      }
    };
  }
}
