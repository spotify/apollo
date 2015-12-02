/*
 * -\-\-
 * Spotify Apollo Jetty HTTP Server Module
 * --
 * Copyright (C) 2013 - 2015 Spotify AB
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
package com.spotify.apollo.http.server;

import com.google.common.collect.ImmutableMap;

import com.spotify.apollo.Request;

import org.junit.Test;

import java.util.Map;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class HttpRequestTest {

  private Request requestWithHeader(String uri, String header, String value) {
    return HttpRequest.create("GET", uri, Optional.empty(), Optional.empty(),
                              ImmutableMap.of(), ImmutableMap.of(header, value));
  }

  @Test
  public void shouldMergeHeaders() throws Exception {
    Map<String, String> newHeaders = ImmutableMap.of("newHeader", "value1", "newHeader2", "value2");

    assertThat(requestWithHeader("/foo", "old", "value").withHeaders(newHeaders).headers(),
               is(ImmutableMap.of("old", "value",
                                  "newHeader", "value1",
                                  "newHeader2", "value2")));
  }

  @Test
  public void shouldReplaceExistingHeader() throws Exception {
    Map<String, String> newHeaders = ImmutableMap.of("newHeader", "value1", "old", "value2");

    assertThat(requestWithHeader("/foo", "old", "value").withHeaders(newHeaders).headers(),
               is(ImmutableMap.of("old", "value2",
                                  "newHeader", "value1")));
  }

  @Test
  public void shouldClearHeaders() throws Exception {
    assertThat(requestWithHeader("/foo", "old", "value").clearHeaders().headers(),
               is(ImmutableMap.of()));
  }
}
