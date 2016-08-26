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

import com.spotify.apollo.Request;
import com.spotify.apollo.Response;
import com.spotify.apollo.Status;
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
import java.util.stream.Collectors;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletResponse;

import okio.ByteString;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AsyncContextOngoingRequestTest {
  private final TestLogger testLogger = TestLoggerFactory.getTestLogger(AsyncContextOngoingRequest.class);

  private MockHttpServletResponse response;

  @Mock
  AsyncContext asyncContext;
  @Mock
  javax.servlet.ServletOutputStream outputStream;

  @Rule
  public TestLoggerFactoryResetRule testLoggerFactoryResetRule = new TestLoggerFactoryResetRule();
  private AsyncContextOngoingRequest ongoingRequest;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    response = new MockHttpServletResponse();

    when(asyncContext.getResponse()).thenReturn(response);

    ongoingRequest = new AsyncContextOngoingRequest(
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
    ongoingRequest.reply(Response.forStatus(Status.IM_A_TEAPOT).withPayload(ByteString.encodeUtf8("hi there")));

    assertThat(response.getStatus(), is(Status.IM_A_TEAPOT.code()));
    assertThat(response.getErrorMessage(), is(Status.IM_A_TEAPOT.reasonPhrase()));
    assertThat(response.getContentAsString(), is("hi there"));
  }

  @Test
  public void shouldRespond500ForDrop() throws Exception {
    ongoingRequest.drop();

    verify(asyncContext).complete();
    assertThat(response.getStatus(), is(500));
    assertThat(response.getErrorMessage(), is("dropped"));
  }
}