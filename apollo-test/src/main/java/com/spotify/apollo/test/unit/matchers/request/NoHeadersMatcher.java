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
package com.spotify.apollo.test.unit.matchers.request;

import com.spotify.apollo.Request;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class NoHeadersMatcher extends TypeSafeMatcher<Request> {
  @Override
  protected boolean matchesSafely(final Request item) {
    return item.headers().asMap().isEmpty();
  }

  @Override
  protected void describeMismatchSafely(final Request item, final Description mismatchDescription) {
    mismatchDescription.appendText("the request had headers: ")
        .appendValueList("[", ", ", "]", item.headers().asMap().entrySet());
  }

  @Override
  public void describeTo(final Description description) {
    description.appendText("a request with no headers");
  }
}
