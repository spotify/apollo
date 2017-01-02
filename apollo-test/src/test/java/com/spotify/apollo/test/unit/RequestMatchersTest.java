/*
 * -\-\-
 * Spotify Apollo Testing Helpers
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
package com.spotify.apollo.test.unit;

import com.spotify.apollo.Request;

import org.hamcrest.Matcher;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertThat;

public class RequestMatchersTest {

  @Test
  public void shouldMatchUri() throws Exception {
    Matcher<Request> requestMatcher = RequestMatchers.uri("http://hi");
    assertThat(requestMatcher.matches(Request.forUri("http://hi")), is(true));
    assertThat(requestMatcher.matches(Request.forUri("http://hithere")), is(false));
    assertThat(requestMatcher.matches(Request.forUri("http://hi/there")), is(false));
    assertThat(requestMatcher.matches(Request.forUri("http://hey")), is(false));
  }

  @Test
  public void shouldMatchUriMatcher() throws Exception {
    Matcher<Request> requestMatcher = RequestMatchers.uri(startsWith("http://hi"));
    assertThat(requestMatcher.matches(Request.forUri("http://hi")), is(true));
    assertThat(requestMatcher.matches(Request.forUri("http://hithere")), is(true));
    assertThat(requestMatcher.matches(Request.forUri("http://hi/there")), is(true));
    assertThat(requestMatcher.matches(Request.forUri("http://hey")), is(false));
  }

  @Test
  public void shouldMatchMethod() throws Exception {
    Matcher<Request> requestMatcher = RequestMatchers.method("POST");

    assertThat(requestMatcher.matches(Request.forUri("http://hi")), is(false));
    assertThat(requestMatcher.matches(Request.forUri("http://hi", "POST")), is(true));
    assertThat(requestMatcher.matches(Request.forUri("http://hi", "GET")), is(false));
  }

  @Test
  public void shouldMatchService() throws Exception {
    final Matcher<Request> serviceMatcher = RequestMatchers.service("my-service");
    final Request request = Request.forUri("http://irrelevant");

    assertThat("No service", request, not(serviceMatcher));
    assertThat("Service matches", request.withService("my-service"), serviceMatcher);
    assertThat("Service mismatches", request.withService("not-my-service"), not(serviceMatcher));
  }

  @Test
  public void shouldMatchServiceMatcher() throws Exception {
    final Request request = Request.forUri("http://irrelevant");

    assertThat(request, RequestMatchers.service(nullValue(String.class)));
    assertThat(request.withService("my-service"), RequestMatchers.service(startsWith("my")));
    assertThat(request.withService("not-my-service"), not(RequestMatchers.service(endsWith("."))));
  }
}
