/*
 * Copyright (c) 2013-2015 Spotify AB
 */

package com.spotify.apollo.request;

import com.spotify.apollo.Request;
import com.spotify.apollo.RequestContext;
import com.spotify.apollo.dispatch.Endpoint;
import com.spotify.apollo.dispatch.EndpointInfo;
import com.spotify.apollo.meta.IncomingCallsGatherer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TrackingEndpointRunnableFactoryTest {

  @Mock IncomingCallsGatherer incomingCallsGatherer;
  @Mock RequestTracker requestTracker;

  @Mock OngoingRequest ongoingRequest;
  @Mock RequestContext requestContext;
  @Mock Endpoint endpoint;
  @Mock EndpointInfo info;

  @Mock EndpointRunnableFactory delegate;
  @Mock Runnable delegateRunnable;

  TrackingEndpointRunnableFactory endpointRunnableFactory;

  @Before
  public void setUp() throws Exception {
    when(ongoingRequest.request()).thenReturn(Request.forUri("http://foo"));
    when(endpoint.info()).thenReturn(info);
    when(info.getName()).thenReturn("foo");

    when(delegate.create(any(), any(), any())).thenReturn(delegateRunnable);

    endpointRunnableFactory = new TrackingEndpointRunnableFactory(
        delegate, incomingCallsGatherer, requestTracker);
  }

  @Test
  public void shouldRunDelegate() throws Exception {
    endpointRunnableFactory.create(ongoingRequest, requestContext, endpoint).run();

    verify(delegateRunnable).run();
  }

  @Test
  public void shouldGatherCalls() throws Exception {
    endpointRunnableFactory.create(ongoingRequest, requestContext, endpoint).run();

    verify(incomingCallsGatherer).gatherIncomingCall(eq(ongoingRequest), eq(endpoint));
  }
}
