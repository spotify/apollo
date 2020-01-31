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

import okio.ByteString;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

public final class ByteStringMatchers {
  private ByteStringMatchers() {
  }

  /**
   * Matches a {@link ByteString} with utf8 value matching the specified {@link Matcher}
   * @param matcher The matcher matching the expected {@link String} value
   * @return A {@link Matcher} matching ByteStrings utf8 value
   */
  public static Matcher<ByteString> utf8(Matcher<String> matcher) {
    return new FeatureMatcher<ByteString, String>(matcher, "ByteString.utf8", "utf8 value") {
      @Override
      protected String featureValueOf(final ByteString actual) {
        return actual.utf8();
      }
    };
  }
}
