/*
 * -\-\-
 * Spotify Apollo API Interfaces
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
package com.spotify.apollo.route;

import com.google.common.collect.ImmutableMap;

import com.spotify.apollo.RequestContext;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class SyncHandlerTest {

  @Test
  public void shouldMap() throws Exception {
    SyncHandler<String> a = ctx -> "hello";
    SyncHandler<Integer> b = a.map(String::length);

    int helloLen = b.invoke(TestContext.empty());
    assertThat(helloLen, is("hello".length()));
  }

  @Test
  public void shouldFlatMap() throws Exception {
    RequestContext context = TestContext.forPathArgs(ImmutableMap.of("foo", "bar", "bar", "baz"));

    SyncHandler<String> a = ctx -> ctx.pathArgs().get("foo");
    SyncHandler<String> b = a.flatMap(fooVal -> ctx -> ctx.pathArgs().get(fooVal));

    String baz = b.invoke(context);
    assertThat(baz, is("baz"));
  }
}
