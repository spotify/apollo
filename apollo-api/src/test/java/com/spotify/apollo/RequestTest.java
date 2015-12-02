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

import com.google.common.collect.ImmutableList;

import org.junit.Test;

import java.util.Optional;

import okio.ByteString;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class RequestTest {

  private Request request(String uri) {
    return Request.forUri(uri);
  }

  private Request requestWithHeader(String uri, String header, String value) {
    return Request.forUri(uri).withHeader(header, value);
  }

  private Request requestWithPayload(String uri, ByteString payload) {
    return Request.forUri(uri).withPayload(payload);
  }

  @Test
  public void shouldReturnNullForMissingParameterWithDefaultMethod() throws Exception {
    assertThat(request("/foo").parameter("missing"), is(Optional.empty()));
  }

  @Test
  public void shouldReturnNullForMissingParameter() throws Exception {
    assertThat(request("/foo").parameter("missing"), is(Optional.empty()));
  }

  @Test
  public void shouldReturnFirstParameterValueWithDefaultImplementation() throws Exception {
    assertThat(request("/foo?name=value1&name=value2").parameter("name"),
               is(Optional.of("value1")));
  }

  @Test
  public void shouldReturnFirstParameterValue() throws Exception {
    assertThat(request("/foo?name=value1&name=value2").parameter("name"),
               is(Optional.of("value1")));
  }

  @Test
  public void shouldReturnAllParameterValues() throws Exception {
    assertThat(request("/foo?name=value1&name=value2").parameters().get("name"),
               is(ImmutableList.of("value1", "value2")));
  }

  @Test
  public void shouldReturnNullForMissingHeaderWithDefaultMethod() throws Exception {
    assertThat(request("/foo").header("missing"), is(Optional.empty()));
  }

  @Test
  public void shouldReturnNullForMissingHeader() throws Exception {
    assertThat(request("/foo").header("missing"),
               is(Optional.empty()));
  }

  @Test
  public void shouldReturnHeaderWithDefaultMethod() throws Exception {
    assertThat(requestWithHeader("/foo", "header", "value").header("header"),
               is(Optional.of("value")));
  }

  @Test
  public void shouldReturnHeader() throws Exception {
    assertThat(requestWithHeader("/foo", "header", "value").header("header"),
               is(Optional.of("value")));
  }

  @Test
  public void shouldReturnNoPayload() throws Exception {
    assertThat(request("/foo").payload(),
               is(Optional.empty()));
  }

  @Test
  public void shouldReturnPayload() throws Exception {
    ByteString payload = ByteString.encodeUtf8("payload");
    assertThat(requestWithPayload("/foo", payload).payload(),
               is(Optional.of(payload)));
  }

  @Test
  public void shouldAllowModifyingUri() throws Exception {
    assertThat(request("/foo").withUri("/fie").uri(), is("/fie"));
  }
}
