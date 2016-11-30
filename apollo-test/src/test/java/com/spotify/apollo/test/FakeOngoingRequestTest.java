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
package com.spotify.apollo.test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import com.spotify.apollo.Request;
import java.time.Instant;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;

public class FakeOngoingRequestTest {

  @Test
  public void shouldReturnRecentArrivalTime() throws Exception {
    FakeOngoingRequest request = new FakeOngoingRequest(Request.forUri("http://foo"));

    assertThat(request.metadata().arrivalTime(), is(recent()));
  }

  private Matcher<Instant> recent() {
    return new TypeSafeMatcher<Instant>() {
      @Override
      protected boolean matchesSafely(Instant item) {
        return item.isAfter(Instant.now().minusSeconds(1));
      }

      @Override
      public void describeTo(Description description) {
        description.appendText(
            "an instant at most a second older than the current timestamp (" + Instant.now() + ")");
      }
    };
  }
}
