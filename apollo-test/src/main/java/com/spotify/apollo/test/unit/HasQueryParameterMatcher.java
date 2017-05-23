/*
 * -\-\-
 * Spotify Apollo Testing Helpers
 * --
 * Copyright (C) 2013 - 2017 Spotify AB
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
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import java.util.List;

class HasQueryParameterMatcher extends TypeSafeDiagnosingMatcher<Request> {
  private final String key;
  private final Matcher<Iterable<? extends String>> matcher;

  private HasQueryParameterMatcher(final String key, final Matcher<Iterable<? extends String>> matcher) {
    this.key = key;
    this.matcher = matcher;
  }

  static HasQueryParameterMatcher hasQueryParameter(final String key,
                                                    final Matcher<Iterable<? extends String>> matcher) {
    return new HasQueryParameterMatcher(key, matcher);
  }

  @Override
  protected boolean matchesSafely(final Request item, final Description mismatchDescription) {
    if (!item.parameters().containsKey(key)) {
      mismatchDescription.appendText("Request did not have query parameter for key ")
          .appendValue(key)
          .appendText(". Request had query parameters: ")
          .appendValueList("[", ", ", "]", item.parameters().entrySet());
      return false;
    }

    final List<String> value = item.parameters().get(key);
    if (!matcher.matches(value)) {
      mismatchDescription.appendText("value ")
          .appendValueList("[", ", ", "]", value)
          .appendText(" did not match: ");
      matcher.describeMismatch(value, mismatchDescription);
      return false;
    }

    return true;
  }

  @Override
  public void describeTo(final Description description) {
    description.appendText("Request with a query parameter for key ")
        .appendValue(key)
        .appendText(" matching ")
        .appendDescriptionOf(matcher);
  }

}
