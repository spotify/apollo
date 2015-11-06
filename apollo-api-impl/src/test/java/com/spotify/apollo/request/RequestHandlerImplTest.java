/*
 * Copyright (c) 2013-2015 Spotify AB
 */

package com.spotify.apollo.request;

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

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;

import okio.ByteString;

import static com.spotify.apollo.Response.forStatus;
import static com.spotify.apollo.Status.INTERNAL_SERVER_ERROR;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RequestHandlerImplTest {

  @Mock RequestRunnableFactory requestFactory;
  @Mock EndpointRunnableFactory endpointFactory;

  @Mock RequestRunnable requestRunnable;
  @Mock Runnable runnable;

  @Mock OngoingRequest ongoingRequest;
  @Mock RuleMatch<Endpoint> match;
  @Mock Endpoint endpoint;
  @Mock EndpointInfo info;

  @Captor ArgumentCaptor<BiConsumer<OngoingRequest, RuleMatch<Endpoint>>> continuationCaptor;

  RequestHandlerImpl requestHandler;

  @Before
  public void setUp() throws Exception {
    IncomingRequestAwareClient client = new NoopClient();

    when(ongoingRequest.request()).thenReturn(Request.forUri("http://foo"));
    when(requestFactory.create(any())).thenReturn(requestRunnable);
    when(endpointFactory.create(eq(ongoingRequest), any(RequestContext.class), eq(endpoint)))
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

  private static class NoopClient implements IncomingRequestAwareClient {

    @Override
    public CompletionStage<Response<ByteString>> send(Request request, Optional<Request> incoming) {
      throw new UnsupportedOperationException();
    }
  }
}
