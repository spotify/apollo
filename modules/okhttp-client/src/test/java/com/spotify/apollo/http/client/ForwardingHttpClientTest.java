/*
 * -\-\-
 * Spotify Apollo okhttp Client Module
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
package com.spotify.apollo.http.client;

import com.spotify.apollo.Request;
import com.spotify.apollo.Response;

import org.junit.Test;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import okio.ByteString;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class ForwardingHttpClientTest {

  @Test
  public void testSendHttp() throws Exception {
    final ForwardingHttpClient sut = ForwardingHttpClient.create(
        (request, incoming) -> {
          fail("Wrong client called");
          return null; // Unreachable
        },
        (request, incoming) -> CompletableFuture.completedFuture(
            Response.forPayload(ByteString.encodeUtf8("ok"))));

    final Response<ByteString> response =
        sut.send(Request.forUri("http://spoti.fi/bar"), Optional.empty()).toCompletableFuture()
            .get();

    assertThat(response.payload(), is(Optional.of(ByteString.encodeUtf8("ok"))));
  }

  @Test
  public void testSendOther() throws Exception {
    final ForwardingHttpClient sut = ForwardingHttpClient.create(
        (request, incoming) -> CompletableFuture.completedFuture(
            Response.forPayload(ByteString.encodeUtf8("ok"))),
        (request, incoming) -> {
          fail("Wrong client called");
          return null; // Unreachable
        });

    final Response<ByteString> response =
        sut.send(Request.forUri("teapot:stumpton231"), Optional.empty()).toCompletableFuture()
            .get();

    assertThat(response.payload(), is(Optional.of(ByteString.encodeUtf8("ok"))));
  }
}
