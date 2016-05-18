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

import org.junit.Test;

import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;

public class HeadersValueTest {
  private Map<String, String> headers;

  @Test
  public void shouldReuseMapIfAlreadyLowerCase() throws Exception {
    headers = ImmutableMap.of("key", "value", "another-key", "val");

    assertThat(HeadersValue.create(headers).asMap(), is(sameInstance(headers)));
  }

  @Test
  public void shouldConvertKeysToLowerCase() throws Exception {
    headers = ImmutableMap.of("KEY", "value", "Another-Key", "val");

    assertThat(HeadersValue.create(headers).asMap(),
               equalTo(ImmutableMap.of("key", "value", "another-key", "val")));
  }
}