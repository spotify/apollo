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
import com.spotify.apollo.Status;
import com.spotify.apollo.request.OngoingRequest;
import com.spotify.apollo.request.RequestHandler;
import com.spotify.apollo.request.ServerInfo;
import com.spotify.apollo.request.ServerInfos;

import org.eclipse.jetty.server.HttpChannel;
import org.eclipse.jetty.server.HttpInput;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import uk.org.lidalia.slf4jext.Level;
import uk.org.lidalia.slf4jtest.LoggingEvent;
import uk.org.lidalia.slf4jtest.TestLogger;
import uk.org.lidalia.slf4jtest.TestLoggerFactory;
import uk.org.lidalia.slf4jtest.TestLoggerFactoryResetRule;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletResponse;

import io.netty.handler.codec.http.QueryStringDecoder;
import okio.ByteString;

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toMap;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ApolloRequestHandlerTest {
  private ApolloRequestHandler requestHandler;

  private FakeRequestHandler delegate;
  private MockHttpServletRequest httpServletRequest;
  private MockHttpServletResponse response;
  private Duration requestTimeout;
  private final TestLogger testLogger = TestLoggerFactory.getTestLogger(ApolloRequestHandler.class);
  private org.eclipse.jetty.server.Request baseRequest;

  @Mock
  AsyncContext asyncContext;
  @Mock
  javax.servlet.ServletOutputStream outputStream;
  @Mock
  private HttpChannel httpChannel;
  @Mock
  private HttpInput httpInput;


  @Rule
  public TestLoggerFactoryResetRule testLoggerFactoryResetRule = new TestLoggerFactoryResetRule();


  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    delegate = new FakeRequestHandler();
    response = new MockHttpServletResponse();
    ServerInfo serverInfo = ServerInfos.create("id", InetSocketAddress.createUnresolved("localhost", 80));

    requestTimeout = Duration.ofMillis(8275523);
    requestHandler = new ApolloRequestHandler(serverInfo, delegate, requestTimeout);

    httpServletRequest = mockRequest("PUT",
                                     "http://somehost/a/b?q=abc&b=adf&q=def",
                                     emptyMap());
    // Request is extremely complex, and there doesn't seem to be a fake implementation that can
    // be used in tests, so resorting to spying on it.
    baseRequest = spy(new org.eclipse.jetty.server.Request(httpChannel, httpInput));
    doReturn(asyncContext).when(baseRequest).startAsync();
    when(asyncContext.getResponse()).thenReturn(response);
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
  public void shouldLogWarningOnErrorWritingResponse() throws Exception {
    HttpServletResponse spy = spy(response);
    when(asyncContext.getResponse()).thenReturn(spy);
    doReturn(outputStream).when(spy).getOutputStream();
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
        .filter(event -> event.getLevel() == Level.WARN)
        .filter(event -> event.getMessage().contains("Failed to write response"))
        .collect(Collectors.toList());

    assertThat(events, hasSize(1));
  }

  // note: this test may fail when running in IntelliJ, due to
  // https://youtrack.jetbrains.com/issue/IDEA-122783
  @Test
  public void shouldLogWarningOnFailureToGetAsyncContextResponse() throws Exception {
    when(asyncContext.getResponse()).thenThrow(new IllegalStateException("context completed test"));

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

    List<String> events = testLogger.getLoggingEvents().stream()
        .filter(event -> event.getLevel() == Level.WARN)
        .map(LoggingEvent::getMessage)
        .collect(Collectors.toList());

    assertThat(events, hasSize(1));
    assertThat(events, hasItem(containsString("Error sending response")));
  }

  // note: this test may fail when running in IntelliJ, due to
  // https://youtrack.jetbrains.com/issue/IDEA-122783
  @Test
  public void shouldLogWarningOnFailureToCompleteAsyncContext() throws Exception {
    when(asyncContext.getResponse()).thenReturn(response);
    doThrow(new IllegalStateException("completed test")).when(asyncContext).complete();

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

    List<String> events = testLogger.getLoggingEvents().stream()
        .filter(event -> event.getLevel() == Level.WARN)
        .map(LoggingEvent::getMessage)
        .collect(Collectors.toList());

    assertThat(events, hasSize(1));
    assertThat(events, hasItem(containsString("Error sending response")));
  }

  @Test
  public void shouldForwardRepliesToJetty() throws Exception {
    requestHandler.handle("/floop", baseRequest, httpServletRequest, response);

    delegate.replyLast(Response.forStatus(Status.IM_A_TEAPOT));

    verify(asyncContext).complete();
    assertThat(response.getStatus(), is(Status.IM_A_TEAPOT.code()));
    assertThat(response.getErrorMessage(), is(Status.IM_A_TEAPOT.reasonPhrase()));
  }

  // I would prefer to test this in a less implementation-dependent way (validating that a timeout
  // is actually sent, rather than that a particular listener is registered), but the servlet APIs
  // aren't designed that way.
  @Test
  public void shouldRegisterTimeoutListenerWithContext() throws Exception {
    requestHandler.handle("/floop", baseRequest, httpServletRequest, response);

    verify(asyncContext).addListener(TimeoutListener.getInstance());
  }

  // I would prefer to test this in a less implementation-dependent way (validating that a timeout
  // is actually sent, rather than that a particular listener is registered), but the servlet APIs
  // aren't designed that way.
  @Test
  public void shouldSetTimeout() throws Exception {
    requestHandler.handle("/floop", baseRequest, httpServletRequest, response);

    verify(asyncContext).setTimeout(requestTimeout.toMillis());
  }

  @Test
  public void shouldRespond500ForDrop() throws Exception {
    requestHandler.handle("/floop", baseRequest, httpServletRequest, response);

    delegate.dropLast();

    verify(asyncContext).complete();
    assertThat(response.getStatus(), is(500));
    assertThat(response.getErrorMessage(), is("dropped"));
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

  private static class FakeRequestHandler implements RequestHandler {
    private final LinkedList<OngoingRequest> requests = new LinkedList<>();

    @Override
    public void handle(OngoingRequest ongoingRequest) {
      requests.add(ongoingRequest);
    }

    void replyLast(Response<ByteString> response) {
      requests.getLast().reply(response);
    }

    void dropLast() {
      requests.getLast().drop();
    }
  }
}
