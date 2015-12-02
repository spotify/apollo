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

import com.google.common.collect.ImmutableList;

import com.spotify.apollo.request.RequestHandler;
import com.spotify.apollo.request.ServerInfo;
import com.spotify.apollo.request.ServerInfos;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.netty.handler.codec.http.QueryStringDecoder;
import okio.ByteString;

import static java.util.stream.Collectors.toMap;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ApolloRequestHandlerTest {
  ApolloRequestHandler requestHandler;

  @Mock
  RequestHandler mockDelegate;

  MockHttpServletRequest httpServletRequest;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    ServerInfo serverInfo = ServerInfos.create("id", InetSocketAddress.createUnresolved("localhost", 80));

    requestHandler = new ApolloRequestHandler(serverInfo, mockDelegate);

    httpServletRequest = mockRequest("PUT", "http://somehost/a/b?q=abc&b=adf&q=def");
  }

  @Test
  public void shouldExtractCorrectMethod() throws Exception {
    assertThat(requestHandler.asApolloRequest(httpServletRequest).method(),
               is("PUT"));
  }

  @Test
  public void shouldExtractCompleteUri() throws Exception {
    assertThat(requestHandler.asApolloRequest(httpServletRequest).uri(),
               is("http://somehost/a/b?q=abc&b=adf&q=def"));
  }

  @Test
  public void shouldExtractFirstQueryParameter() throws Exception {
    assertThat(requestHandler.asApolloRequest(httpServletRequest).parameter("q"),
               is(Optional.of("abc")));
  }

  @Test
  public void shouldExtractAllParameters() throws Exception {
    assertThat(requestHandler.asApolloRequest(httpServletRequest).parameters().get("q"),
               is(ImmutableList.of("abc", "def")));
  }

  @Test
  public void shouldReturnEmptyForMissingParameter() throws Exception {
    assertThat(requestHandler.asApolloRequest(httpServletRequest).parameter("p"),
               is(Optional.<String>empty()));
  }

  @Test
  public void shouldReturnEmptyPayloadWhenMissing() throws Exception {
    assertThat(requestHandler.asApolloRequest(httpServletRequest).payload(),
               is(Optional.empty()));
  }

  @Test
  public void shouldReturnPayloadWhenPresent() throws Exception {
    httpServletRequest.setContent("hi there".getBytes(StandardCharsets.UTF_8));

    assertThat(requestHandler.asApolloRequest(httpServletRequest).payload(),
               is(Optional.of(ByteString.encodeUtf8("hi there"))));
  }

  private MockHttpServletRequest mockRequest(String method, String requestURI) {
    QueryStringDecoder decoder = new QueryStringDecoder(requestURI);

    final MockHttpServletRequest mockHttpServletRequest =
        new MockHttpServletRequest(method, decoder.path());

    mockHttpServletRequest.setParameters(
        decoder.parameters().entrySet().stream()
            .collect(toMap(Map.Entry::getKey, e -> {
              List<String> value = e.getValue();
              return value.toArray(new String[value.size()]);
            })));
    mockHttpServletRequest.setQueryString(requestURI.replace(decoder.path() + "?", ""));

    return mockHttpServletRequest;
  }
}
