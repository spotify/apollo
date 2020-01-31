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
import org.hamcrest.TypeSafeMatcher;

import java.util.Optional;

class HeaderMatcher extends TypeSafeMatcher<Request> {
  private final String key;
  private final Matcher<String> valueMatcher;

  HeaderMatcher(final String key, final Matcher<String> valueMatcher) {
    this.key = key;
    this.valueMatcher = valueMatcher;
  }

  @Override
  protected boolean matchesSafely(Request item) {
    return item.header(key).filter(valueMatcher::matches).isPresent();
  }

  @Override
  protected void describeMismatchSafely(final Request item, final Description mismatchDescription) {
    final Optional<String> header = item.header(key);
    if (header.isPresent()) {
      mismatchDescription.appendText("Header ")
          .appendValue(key)
          .appendText(" had value ")
          .appendText(header.get())
          .appendText(" that did not match ");
      valueMatcher.describeMismatch(item, mismatchDescription);
    } else {
      mismatchDescription.appendText("Header ")
          .appendValue(key)
          .appendText(" was not present. Headers present: ")
          .appendValueList("[", ", ", "]", item.headerEntries());
    }
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("A request that has header ")
        .appendValue(key)
        .appendText(" with value ")
        .appendDescriptionOf(valueMatcher);
  }
}
