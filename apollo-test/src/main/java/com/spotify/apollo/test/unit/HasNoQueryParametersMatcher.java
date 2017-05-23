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
import org.hamcrest.TypeSafeDiagnosingMatcher;

class HasNoQueryParametersMatcher extends TypeSafeDiagnosingMatcher<Request> {
  private static final HasNoQueryParametersMatcher INSTANCE = new HasNoQueryParametersMatcher();

  static HasNoQueryParametersMatcher hasNoQueryParameters() {
    return INSTANCE;
  }

  private HasNoQueryParametersMatcher() {
  }

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
}
