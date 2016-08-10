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
import com.google.common.collect.ImmutableMap;

import com.spotify.apollo.Request;
import com.spotify.apollo.Response;
import com.spotify.apollo.request.OngoingRequest;
import com.spotify.apollo.request.RequestHandler;
import com.spotify.apollo.request.ServerInfo;
import com.spotify.apollo.request.ServerInfos;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;

import uk.org.lidalia.slf4jext.Level;
import uk.org.lidalia.slf4jtest.LoggingEvent;
import uk.org.lidalia.slf4jtest.TestLogger;
import uk.org.lidalia.slf4jtest.TestLoggerFactory;
import uk.org.lidalia.slf4jtest.TestLoggerFactoryResetRule;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.AsyncContext;
import javax.servlet.RequestDispatcher;

import io.netty.handler.codec.http.QueryStringDecoder;
import okio.ByteString;

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toMap;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ApolloRequestHandlerTest {
  ApolloRequestHandler requestHandler;

  @Mock
  RequestHandler mockDelegate;
  @Mock
  AsyncContext asyncContext;
  @Mock
  javax.servlet.http.HttpServletResponse response;
  @Mock
  javax.servlet.ServletOutputStream outputStream;

  MockHttpServletRequest httpServletRequest;

  @Rule
  public TestLoggerFactoryResetRule testLoggerFactoryResetRule = new TestLoggerFactoryResetRule();

  private final TestLogger testLogger = TestLoggerFactory.getTestLogger(ApolloRequestHandler.class);

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    ServerInfo serverInfo = ServerInfos.create("id", InetSocketAddress.createUnresolved("localhost", 80));

    requestHandler = new ApolloRequestHandler(serverInfo, mockDelegate);

    httpServletRequest = mockRequest("PUT",
                                     "http://somehost/a/b?q=abc&b=adf&q=def",
                                     emptyMap());
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

  @Test
  public void shouldExtractCallingServiceFromHeader() throws Exception {
    httpServletRequest = mockRequest("PUT",
                                     "http://somehost/a/b?q=abc&b=adf&q=def",
                                     ImmutableMap.of("X-Calling-Service", "testservice"));

    assertThat(requestHandler.asApolloRequest(httpServletRequest).service(),
               is(Optional.of("testservice")));
  }

  @Test
  public void shouldNotExtractCallingServiceFromEmptyHeader() throws Exception {
    httpServletRequest = mockRequest("PUT",
                                     "http://somehost/a/b?q=abc&b=adf&q=def",
                                     ImmutableMap.of("X-Calling-Service", ""));

    assertThat(requestHandler.asApolloRequest(httpServletRequest).service(),
               is(Optional.empty()));
  }

  @Test
  public void shouldHandleMissingCallingServiceHeader() throws Exception {
    httpServletRequest = mockRequest("PUT",
                                     "http://somehost/a/b?q=abc&b=adf&q=def",
                                     emptyMap());

    assertThat(requestHandler.asApolloRequest(httpServletRequest).service(),
               is(Optional.empty()));
  }

  // note: this test may fail when running in IntelliJ, due to
  // https://youtrack.jetbrains.com/issue/IDEA-122783
  @Test
  public void shouldLogErrorWritingResponse() throws Exception {
    when(asyncContext.getResponse()).thenReturn(response);
    when(response.getOutputStream()).thenReturn(outputStream);
    doThrow(new IOException("expected")).when(outputStream).write(any(byte[].class));

    ApolloRequestHandler.AsyncContextOngoingRequest ongoingRequest = new ApolloRequestHandler.AsyncContextOngoingRequest(
        new ServerInfo() {
          @Override
          public String id() {
            return "14";
          }

          @Override
          public InetSocketAddress socketAddress() {
            return InetSocketAddress.createUnresolved("localhost", 888);
          }
        },
        Request.forUri("http://localhost:888"),
        asyncContext, 9123
    );

    ongoingRequest.reply(Response.forPayload(ByteString.encodeUtf8("floop")));

    List<LoggingEvent> events = testLogger.getLoggingEvents().stream()
        .filter(event -> event.getLevel() == Level.ERROR)
        .filter(event -> event.getMessage().contains("Failed to write response"))
        .collect(Collectors.toList());

    assertThat(events, hasSize(1));
  }

  @Test
  public void shouldNotForwardErrorDispatch() throws Exception {
    org.eclipse.jetty.server.Request baseRequest = errorDispatchRequest();
    requestHandler.handle("/floop", baseRequest, httpServletRequest, response);

    verify(mockDelegate, never()).handle(any(OngoingRequest.class));
  }

  @Test
  public void shouldSendErrorResponseFromDispatch() throws Exception {
    org.eclipse.jetty.server.Request baseRequest = errorDispatchRequest();
    requestHandler.handle("/floop", baseRequest, httpServletRequest, response);

    verify(response).sendError(500, "tje error messaje");
  }

  private org.eclipse.jetty.server.Request errorDispatchRequest() {
    org.eclipse.jetty.server.Request baseRequest = new org.eclipse.jetty.server.Request(null, null);

    // from HttpChannel ERROR_DISPATCH case; using this to detect that the request has in fact
    // errored out already
    baseRequest.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 500);
    baseRequest.setAttribute(RequestDispatcher.ERROR_MESSAGE, "tje error messaje");
    baseRequest.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, "/floop");
    return baseRequest;
  }

  private MockHttpServletRequest mockRequest(
      String method, String requestURI, Map<String, String> headers) {
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

    headers.forEach(mockHttpServletRequest::addHeader);

    return mockHttpServletRequest;
  }
}
