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
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Test;

import static com.spotify.apollo.test.helper.MatchersHelper.assertDoesNotMatch;
import static org.junit.Assert.assertThat;

public class ByteStringMatchersTest {
  @Test
  public void utf8Matcher() throws Exception {
    Matcher<ByteString> sut = ByteStringMatchers.utf8(Matchers.equalTo("he"));

    assertThat(ByteString.encodeUtf8("he"), sut);
    assertDoesNotMatch(ByteString.encodeUtf8("no"), sut);
  }
}
