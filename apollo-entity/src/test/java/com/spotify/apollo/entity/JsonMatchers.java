/*
 * -\-\-
 * Spotify Apollo Entity Middleware
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
package com.spotify.apollo.entity;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import okio.ByteString;

import static com.jayway.jsonassert.JsonAssert.with;

class JsonMatchers {

  static Matcher<ByteString> asStr(Matcher<String> strMatcher) {
    return new TypeSafeMatcher<ByteString>() {
      @Override
      protected boolean matchesSafely(ByteString byteString) {
        return strMatcher.matches(byteString.utf8());
      }

      @Override
      public void describeTo(Description description) {
        strMatcher.describeTo(description);
      }
    };
  }

  static <T> Matcher<String> hasJsonPath(String jsonPath, Matcher<T> matches) {

    return new TypeSafeMatcher<String>() {
      @Override
      protected boolean matchesSafely(String json) {
        try {
          with(json).assertThat(jsonPath, matches);
          return true;
        } catch (AssertionError ae) {
          return false;
        } catch (Exception e) {
          return false;
        }
      }

      @Override
      public void describeTo(Description description) {
        description
            .appendText(" JSON object with a value at node ")
            .appendValue(jsonPath)
            .appendText(" that is ")
            .appendDescriptionOf(matches);
      }
    };
  }

}
