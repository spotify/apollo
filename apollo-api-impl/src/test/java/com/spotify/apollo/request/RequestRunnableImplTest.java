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
package com.spotify.apollo.request;

import com.spotify.apollo.Request;
import com.spotify.apollo.Response;
import com.spotify.apollo.Status;
import com.spotify.apollo.dispatch.Endpoint;
import com.spotify.apollo.dispatch.EndpointInfo;
import com.spotify.apollo.route.ApplicationRouter;
import com.spotify.apollo.route.RuleMatch;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.AbstractMap.SimpleEntry;
import java.util.Collections;
import java.util.Optional;
import java.util.function.BiConsumer;

import okio.ByteString;

import static com.spotify.apollo.Response.forStatus;
import static com.spotify.apollo.Status.INTERNAL_SERVER_ERROR;
import static com.spotify.apollo.Status.NOT_FOUND;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RequestRunnableImplTest {

  @Mock OngoingRequest ongoingRequest;
  @Mock ApplicationRouter<Endpoint> applicationRouter;
  @Mock RuleMatch<Endpoint> match;
  @Mock BiConsumer<OngoingRequest, RuleMatch<Endpoint>> matchContinuation;
  @Mock Endpoint endpoint;
  @Mock EndpointInfo info;
  @Mock Request message;

  @Captor ArgumentCaptor<Response<ByteString>> responseArgumentCaptor;

  RequestRunnableImpl requestRunnable;

  @Before
  public void setUp() throws Exception {
    when(applicationRouter.match(any(Request.class))).thenReturn(Optional.of(match));
    when(ongoingRequest.request()).thenReturn(message);

    requestRunnable = new RequestRunnableImpl(ongoingRequest, applicationRouter);
  }

  @Test public void testRunsMatchedEndpoint() {
    requestRunnable.run(matchContinuation);

    verify(matchContinuation, times(1)).accept(eq(ongoingRequest), eq(match));
  }

  @Test
  public void testMatchingFails() throws Exception {
    when(applicationRouter.match(any(Request.class))).thenReturn(Optional.empty());
    when(applicationRouter.getMethodsForValidRules(any(Request.class)))
        .thenReturn(Collections.emptyList());

    requestRunnable.run(matchContinuation);

    verify(ongoingRequest).reply(forStatus(NOT_FOUND));
  }

  @Test
  public void testWrongMethod() throws Exception {
    when(applicationRouter.match(any(Request.class))).thenReturn(Optional.empty());
    when(applicationRouter.getMethodsForValidRules(any(Request.class)))
        .thenReturn(Collections.singleton("POST"));
    when(message.method()).thenReturn("GET");

    requestRunnable.run(matchContinuation);

    verify(ongoingRequest).reply(responseArgumentCaptor.capture());
    Response<ByteString> reply = responseArgumentCaptor.getValue();
    assertEquals(reply.status(), Status.METHOD_NOT_ALLOWED);
    assertEquals(reply.headerEntries(),
        Collections.singletonList(new SimpleEntry<>("Allow", "OPTIONS, POST")));
  }

  @Test
  public void testWithMethodOptions() throws Exception {
    when(applicationRouter.match(any(Request.class))).thenReturn(Optional.empty());
    when(applicationRouter.getMethodsForValidRules(any(Request.class)))
        .thenReturn(Collections.singleton("POST"));
    when(message.method()).thenReturn("OPTIONS");

    requestRunnable.run(matchContinuation);

    verify(ongoingRequest).reply(responseArgumentCaptor.capture());
    Response<ByteString> response = responseArgumentCaptor.getValue();
    assertThat(response.status(), is(Status.NO_CONTENT));
    assertThat(response.headerEntries(),
        is(Collections.singletonList(new SimpleEntry<>("Allow", "OPTIONS, POST"))));
  }

  @Test
  public void shouldReply500IfApplicationRouterMatchThrows() throws Exception {
    when(applicationRouter.match(any(Request.class))).thenThrow(new RuntimeException("expected"));

    requestRunnable.run(matchContinuation);

    verify(ongoingRequest).reply(forStatus(INTERNAL_SERVER_ERROR));
  }

  @Test
  public void shouldReply500IfApplicationRouterValidMethodsThrows() throws Exception {
    when(applicationRouter.match(any(Request.class))).thenReturn(Optional.empty());
    when(applicationRouter.getMethodsForValidRules(any(Request.class)))
        .thenThrow(new RuntimeException("expected"));

    requestRunnable.run(matchContinuation);

    verify(ongoingRequest).reply(forStatus(INTERNAL_SERVER_ERROR));
  }
}
