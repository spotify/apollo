/*
 * -\-\-
 * Spotify Apollo Jetty HTTP Server Module
 * --
 * Copyright (C) 2013 - 2016 Spotify AB
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
import com.spotify.apollo.Response;
import com.spotify.apollo.Status;
import com.spotify.apollo.request.OngoingRequest;
import com.spotify.apollo.request.ServerInfo;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletResponse;

import uk.org.lidalia.slf4jext.Level;
import uk.org.lidalia.slf4jtest.LoggingEvent;
import uk.org.lidalia.slf4jtest.TestLogger;
import uk.org.lidalia.slf4jtest.TestLoggerFactory;
import uk.org.lidalia.slf4jtest.TestLoggerFactoryResetRule;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletResponse;

import okio.ByteString;

import static com.spotify.apollo.Status.BAD_REQUEST;
import static com.spotify.apollo.Status.IM_A_TEAPOT;
import static com.spotify.apollo.Status.INTERNAL_SERVER_ERROR;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AsyncContextOngoingRequestTest {

  private static final ServerInfo SERVER_INFO = new ServerInfo() {
    @Override
    public String id() {
      return "14";
    }

    @Override
    public InetSocketAddress socketAddress() {
      return InetSocketAddress.createUnresolved("localhost", 888);
    }
  };
  private static final Request REQUEST = Request.forUri("http://localhost:888");
  private static final int ARRIVAL_TIME_NANOS = 9123;
  private static final Response<ByteString>
      DROPPED = Response.forStatus(INTERNAL_SERVER_ERROR.withReasonPhrase("dropped"));

  private final TestLogger testLogger = TestLoggerFactory.getTestLogger(AsyncContextOngoingRequest.class);

  private AsyncContextOngoingRequest ongoingRequest;

  private MockHttpServletResponse response;

  @Mock
  private RequestOutcomeConsumer logger;

  @Mock
  AsyncContext asyncContext;
  @Mock
  javax.servlet.ServletOutputStream outputStream;

  @Rule
  public TestLoggerFactoryResetRule testLoggerFactoryResetRule = new TestLoggerFactoryResetRule();

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    response = new MockHttpServletResponse();

    when(asyncContext.getResponse()).thenReturn(response);

    ongoingRequest = new AsyncContextOngoingRequest(
        SERVER_INFO,
        REQUEST,
        asyncContext,
        ARRIVAL_TIME_NANOS,
        logger,
        ImmutableMap.of());
  }

  // note: this test may fail when running in IntelliJ, due to
  // https://youtrack.jetbrains.com/issue/IDEA-122783
  @Test
  public void shouldLogWarningOnErrorWritingResponse() throws Exception {
    HttpServletResponse spy = spy(response);
    when(asyncContext.getResponse()).thenReturn(spy);
    doReturn(outputStream).when(spy).getOutputStream();
    doThrow(new IOException("expected")).when(outputStream).write(any(byte[].class));

    ongoingRequest.reply(Response.forPayload(ByteString.encodeUtf8("floop")));

    List<LoggingEvent> events = testLogger.getLoggingEvents().stream()
        .filter(event -> event.getLevel() == Level.WARN)
        .filter(event -> event.getMessage().contains("Failed to write response"))
        .collect(Collectors.toList());

    assertThat(events, hasSize(1));
  }

  @Test
  public void shouldCompleteContextOnReply() throws Exception {
    ongoingRequest.reply(Response.forStatus(Status.ACCEPTED));

    verify(asyncContext).complete();
  }

  @Test
  public void shouldReplyOnlyOnce() throws Exception {
    ongoingRequest.reply(Response.forStatus(Status.ACCEPTED));
    ongoingRequest.reply(Response.forStatus(Status.INTERNAL_SERVER_ERROR));

    assertThat(response.getStatus(), is(Status.ACCEPTED.code()));
  }

  @Test
  public void shouldForwardRepliesToJetty() throws Exception {
    ongoingRequest.reply(Response.forStatus(IM_A_TEAPOT)
                             .withPayload(ByteString.encodeUtf8("hi there")));

    assertThat(response.getStatus(), is(IM_A_TEAPOT.code()));
    assertThat(response.getErrorMessage(), is(IM_A_TEAPOT.reasonPhrase()));
    assertThat(response.getContentAsString(), is("hi there"));
  }

  @Test
  public void shouldRespond500ForDrop() throws Exception {
    ongoingRequest.drop();

    verify(asyncContext).complete();
    assertThat(response.getStatus(), is(500));
    assertThat(response.getErrorMessage(), is("dropped"));
  }

  @Test
  public void shouldSendResponsesToConsumer() throws Exception {
    Response<ByteString> hi = Response.forStatus(BAD_REQUEST.withReasonPhrase("hi"));
    ongoingRequest.reply(hi);

    verify(logger).accept(ongoingRequest, Optional.of(hi));
  }

  @Test
  public void shouldSendDropsToConsumer() throws Exception {
    ongoingRequest.drop();

    verify(logger).accept(ongoingRequest, Optional.of(DROPPED));
  }

  @Test
  public void shouldNotAllowOverridingDropHandling() throws Exception {
    OngoingRequest ongoingRequest = new Subclassed(SERVER_INFO, REQUEST, asyncContext, ARRIVAL_TIME_NANOS, logger);

    ongoingRequest.drop();

    verify(logger).accept(ongoingRequest, Optional.of(DROPPED));
  }

  private static class Subclassed extends AsyncContextOngoingRequest {


    Subclassed(ServerInfo serverInfo, Request request,
               AsyncContext asyncContext, long arrivalTimeNanos,
               RequestOutcomeConsumer logger) {
      super(serverInfo, request, asyncContext, arrivalTimeNanos, logger, ImmutableMap.of());
    }

    @Override
    public void reply(Response<ByteString> response) {
      super.reply(Response.forStatus(response.status().withReasonPhrase("overridden")));
    }
  }
}