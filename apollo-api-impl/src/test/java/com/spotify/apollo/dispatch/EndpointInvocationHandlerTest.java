/*
 * -\-\-
 * Spotify Apollo API Implementations
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
package com.spotify.apollo.dispatch;

import com.spotify.apollo.Request;
import com.spotify.apollo.RequestContext;
import com.spotify.apollo.Response;
import com.spotify.apollo.request.OngoingRequest;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.concurrent.CompletableFuture;

import okio.ByteString;

import static com.spotify.apollo.Status.INTERNAL_SERVER_ERROR;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EndpointInvocationHandlerTest {
  EndpointInvocationHandler handler;

  @Mock private OngoingRequest ongoingRequest;
  private Request requestMessage;

  @Mock private RequestContext requestContext;
  @Mock private Endpoint endpoint;
  @Mock private Response<ByteString> response;

  @Captor private ArgumentCaptor<Response<ByteString>> messageArgumentCaptor;

  private CompletableFuture<Response<ByteString>> future;

  @Before
  public void setUp() throws Exception {
    handler = new EndpointInvocationHandler();

    requestMessage = Request.forUri("http://foo/bar").withService("nameless-registry");

    when(ongoingRequest.request()).thenReturn(requestMessage);

    when(requestContext.request()).thenReturn(requestMessage);
    future = new CompletableFuture<>();
  }

  @Test
  public void shouldRespondWithResponseMessageForOk() throws Exception {
    when(endpoint.invoke(any(RequestContext.class)))
             .thenReturn(completedFuture(response));

    handler.handle(ongoingRequest, requestContext, endpoint);

    verify(ongoingRequest).reply(response);
  }

  @Test
  public void shouldRespondWith500ForGeneralException() throws Exception {
    RuntimeException exception = new RuntimeException("expected");
    future.completeExceptionally(exception);
    when(endpoint.invoke(any(RequestContext.class)))
        .thenReturn(future);

    handler.handle(ongoingRequest, requestContext, endpoint);

    verify(ongoingRequest).reply(messageArgumentCaptor.capture());

    assertThat(messageArgumentCaptor.getValue().status().code(),
               equalTo(INTERNAL_SERVER_ERROR.code()));
  }

  @Test
  public void shouldRespondWithDetailMessageForExceptionsToNonClientCallers() throws Exception {
    RuntimeException exception = new RuntimeException("expected");
    when(endpoint.invoke(any(RequestContext.class)))
        .thenReturn(future);
    future.completeExceptionally(exception);

    handler.handle(ongoingRequest, requestContext, endpoint);

    verify(ongoingRequest).reply(messageArgumentCaptor.capture());

    assertThat(messageArgumentCaptor.getValue().status().reasonPhrase(),
               containsString(exception.getMessage()));
  }

  @Test
  public void shouldNotBlockOnFutures() throws Exception {
    when(endpoint.invoke(any(RequestContext.class)))
        .thenReturn(future);

    handler.handle(ongoingRequest, requestContext, endpoint);

    // wait a little to at least make this test flaky if the underlying code is broken
    Thread.sleep(50);

    verifyZeroInteractions(ongoingRequest);

    future.complete(response);

    verify(ongoingRequest).reply(response);
  }

  @Test
  public void shouldUnwrapMultiLineExceptions() throws Exception {
    RuntimeException exception = new RuntimeException("expected\nwith multiple\rlines");
    future.completeExceptionally(exception);

    when(endpoint.invoke(any(RequestContext.class)))
        .thenReturn(future);

    handler.handle(ongoingRequest, requestContext, endpoint);

    verify(ongoingRequest).reply(messageArgumentCaptor.capture());

    assertThat(messageArgumentCaptor.getValue().status().reasonPhrase(),
               containsString("expected"));
    assertThat(messageArgumentCaptor.getValue().status().reasonPhrase(),
               containsString("with multiple"));
    assertThat(messageArgumentCaptor.getValue().status().reasonPhrase(),
               containsString("lines"));
    assertThat(messageArgumentCaptor.getValue().status().reasonPhrase(),
               not(containsString("\r")));
    assertThat(messageArgumentCaptor.getValue().status().reasonPhrase(),
               not(containsString("\n")));
  }

  @Test
  public void shouldRespondWithDetailMessageForSyncExceptionsToNonClientCallers() throws Exception {
    RuntimeException exception = new RuntimeException("expected");
    when(endpoint.invoke(any(RequestContext.class)))
      .thenThrow(exception);

    handler.handle(ongoingRequest, requestContext, endpoint);

    verify(ongoingRequest).reply(messageArgumentCaptor.capture());

    assertThat(messageArgumentCaptor.getValue().status().reasonPhrase(),
        containsString(exception.getMessage()));
  }

  private Matcher<String> nullOrEmpty() {
    return new BaseMatcher<String>() {
      @Override
      public boolean matches(Object item) {
        return item == null || (item instanceof String) && ((String) item).isEmpty();
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("a null or empty string");
      }
    };
  }
}
