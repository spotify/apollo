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

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class AsyncHandlerTest {

  @Test
  public void shouldMap() throws Exception {
    AsyncHandler<String> a = ctx -> completedFuture("hello");
    AsyncHandler<Integer> b = a.map(String::length);

    int helloLen = b.invoke(TestContext.empty()).toCompletableFuture().get();
    assertThat(helloLen, is("hello".length()));
  }

  @Test
  public void shouldFlatMap() throws Exception {
    RequestContext context = TestContext.forPathArgs(ImmutableMap.of("foo", "bar", "bar", "baz"));

    AsyncHandler<String> a = ctx -> completedFuture(ctx.pathArgs().get("foo"));
    AsyncHandler<String> b = a.flatMap(fooVal -> ctx -> completedFuture(ctx.pathArgs().get(fooVal)));

    String baz = b.invoke(context).toCompletableFuture().get();
    assertThat(baz, is("baz"));
  }

  @Test
  public void shouldFlatMapSync() throws Exception {
    RequestContext context = TestContext.forPathArgs(ImmutableMap.of("foo", "bar", "bar", "baz"));

    AsyncHandler<String> a = ctx -> completedFuture(ctx.pathArgs().get("foo"));
    AsyncHandler<String> b = a.flatMapSync(fooVal -> ctx -> ctx.pathArgs().get(fooVal));

    String baz = b.invoke(context).toCompletableFuture().get();
    assertThat(baz, is("baz"));
  }
}
