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

import com.google.common.collect.ImmutableMap;

import com.spotify.apollo.Request;
import com.spotify.apollo.RequestContext;
import com.spotify.apollo.Response;
import com.spotify.apollo.dispatch.Endpoint;
import com.spotify.apollo.dispatch.EndpointInfo;
import com.spotify.apollo.environment.IncomingRequestAwareClient;
import com.spotify.apollo.route.Rule;
import com.spotify.apollo.route.RuleMatch;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;

import okio.ByteString;

import static com.spotify.apollo.Response.forStatus;
import static com.spotify.apollo.Status.INTERNAL_SERVER_ERROR;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RequestHandlerImplTest {

  private static final long ARRIVAL_TIME_NANOS = 4711L;

  @Mock RequestRunnableFactory requestFactory;
  @Mock EndpointRunnableFactory endpointFactory;

  @Mock RequestRunnable requestRunnable;
  @Mock Runnable runnable;

  @Mock OngoingRequest ongoingRequest;
  @Mock RuleMatch<Endpoint> match;
  @Mock Endpoint endpoint;
  @Mock EndpointInfo info;

  @Captor ArgumentCaptor<BiConsumer<OngoingRequest, RuleMatch<Endpoint>>> continuationCaptor;
  @Captor ArgumentCaptor<RequestContext> requestContextCaptor;

  RequestHandlerImpl requestHandler;

  @Before
  public void setUp() throws Exception {
    IncomingRequestAwareClient client = new NoopClient();

    when(ongoingRequest.arrivalTimeNanos()).thenReturn(ARRIVAL_TIME_NANOS);
    when(ongoingRequest.request()).thenReturn(Request.forUri("http://foo"));
    when(requestFactory.create(any())).thenReturn(requestRunnable);
    when(endpointFactory.create(eq(ongoingRequest), requestContextCaptor.capture(), eq(endpoint)))
        .thenReturn(runnable);

    when(match.getRule()).thenReturn(Rule.fromUri("http://foo", "GET", endpoint));
    when(endpoint.info()).thenReturn(info);
    when(info.getName()).thenReturn("foo");

    requestHandler = new RequestHandlerImpl(requestFactory, endpointFactory, client);
  }

  @Test
  public void shouldRunRequestRunnable() throws Exception {
    requestHandler.handle(ongoingRequest);

    verify(requestRunnable).run(any());
  }

  @Test
  public void shouldRunEndpointRunnable() throws Exception {
    requestHandler.handle(ongoingRequest);

    verify(requestRunnable).run(continuationCaptor.capture());

    continuationCaptor.getValue()
        .accept(ongoingRequest, match);

    verify(endpointFactory).create(eq(ongoingRequest), any(RequestContext.class), eq(endpoint));
    verify(runnable).run();
  }

  @Test
  public void shouldReplySafelyForExceptions() throws Exception {
    doThrow(new NullPointerException("expected")).when(requestRunnable).run(any());

    requestHandler.handle(ongoingRequest);

    verify(ongoingRequest).reply(forStatus(INTERNAL_SERVER_ERROR));
  }

  @Test
  public void shouldSetRequestContextArrivalTime() throws Exception {
    requestHandler.handle(ongoingRequest);

    verify(requestRunnable).run(continuationCaptor.capture());

    continuationCaptor.getValue()
        .accept(ongoingRequest, match);

    final RequestContext requestContext = requestContextCaptor.getValue();
    assertThat(requestContext.arrivalTimeNanos(), is(ARRIVAL_TIME_NANOS));
  }

  @Test
  public void shouldCopyRequestMetadataToContext() throws Exception {
    ImmutableMap<String, String> expected = ImmutableMap.of("hi", "ho");
    when(ongoingRequest.metadata()).thenReturn(expected);

    requestHandler.handle(ongoingRequest);

    verify(requestRunnable).run(continuationCaptor.capture());

    continuationCaptor.getValue()
        .accept(ongoingRequest, match);

    final RequestContext requestContext = requestContextCaptor.getValue();
    assertThat(requestContext.metadata(), is(expected));
  }

  private static class NoopClient implements IncomingRequestAwareClient {

    @Override
    public CompletionStage<Response<ByteString>> send(Request request, Optional<Request> incoming) {
      throw new UnsupportedOperationException();
    }
  }
}
