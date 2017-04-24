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
package com.spotify.apollo;

import com.google.common.collect.ImmutableMap;

import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

public class ResponseTest {

  @Test
  public void allowsOverrideHeaderValues() {
    Response<?> response = Response
        .forStatus(Status.OK)
        .withHeader("Content-Type", "application/json")
        .withHeader("Content-Type", "application/protobuf");

    assertEquals(Optional.of("application/protobuf"), response.header("Content-Type"));
  }

  @Test
  public void allowsAddingMultipleHeaders() {
    Map<String, String> headers = ImmutableMap.of(
        "Content-Type", "application/protobuf",
        "Content-Length", "123");

    Response<?> response = Response
        .forStatus(Status.OK)
        .withHeader("Content-Type", "application/json")
        .withHeaders(headers);

    assertEquals(Optional.of("application/protobuf"), response.header("Content-Type"));
    assertEquals(Optional.of("123"), response.header("Content-Length"));
  }

  @Test
  public void shouldReturnHeaderCaseInsensitive() throws Exception {
    assertThat(Response.ok().withHeader("hEaDEr", "value").header("HeadeR"),
        is(Optional.of("value")));
  }

  @Test
  public void shouldReturnHeaderEntriesPreservingLetterCase() throws Exception {
    List<Map.Entry<String, String>> headerEntries = Response.ok().withHeader("HEAder", "value")
        .headerEntries();
    assertThat(headerEntries.size(), is(1));
    assertThat(headerEntries.get(0).getKey(),
        is("HEAder"));
  }

  @Test
  public void shouldReturnHeadersMapPreservingLetterCase() throws Exception {
    Map<String, String> headers = Response.ok().withHeader("HEAder", "value").headers();

    assertThat(headers.get("HEAder"), is("value"));
  }

  @Test
  public void allowsOverrideHeaderValuesInHeadersMap() {
    Response<?> response = Response
        .forStatus(Status.OK)
        .withHeader("Content-Type", "application/json")
        .withHeader("Content-Type", "application/protobuf");

    assertEquals("application/protobuf", response.headers().get("Content-Type"));
  }

  @Test
  public void shouldHaveSingletonOK() throws Exception {
    Response<Object> ok1 = Response.ok();
    Response<Object> ok2 = Response.forStatus(Status.OK);

    assertSame(ok1, ok2);
  }

  @Test
  public void shouldNotIgnoreCustomOk() throws Exception {
    Response<Object> ok1 = Response.ok();
    Response<Object> ok2 = Response.forStatus(new CustomOK());

    assertNotSame(ok1, ok2);
  }

  static class CustomOK implements StatusType {

    @Override
    public int code() {
      return 200;
    }

    @Override
    public String reasonPhrase() {
      return "Is more than OK";
    }

    @Override
    public Family family() {
      return Family.SUCCESSFUL;
    }

    @Override
    public StatusType withReasonPhrase(String reasonPhrase) {
      return this;
    }
  }
}
