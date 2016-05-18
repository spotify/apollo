/*
 * -\-\-
 * Spotify Apollo API Interfaces
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
package com.spotify.apollo;

import com.google.common.collect.ImmutableMap;

import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class HeadersTest {

  private Headers headers;

  @Before
  public void setUp() throws Exception {
    headers = Headers.of(ImmutableMap.of("Fie", "fie", "foO-Fupp", "Fum"));
  }

  @Test
  public void shouldConvertHeaderNamesToLowerCase() throws Exception {
    assertThat(headers.asMap(),
               equalTo(ImmutableMap.of("fie", "fie", "foo-fupp", "Fum")));
  }

  @Test
  public void shouldSupportUpperCaseLookup() throws Exception {
    assertThat(headers.get("FIE"), is(Optional.of("fie")));
  }
}