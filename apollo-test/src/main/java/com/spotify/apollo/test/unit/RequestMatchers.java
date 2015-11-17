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
