/*
 * Copyright (c) 2013-2015 Spotify AB
 */

package com.spotify.apollo.request;

import com.spotify.apollo.RequestContext;
import com.spotify.apollo.dispatch.Endpoint;
import com.spotify.apollo.meta.IncomingCallsGatherer;

/**
 * An {@link EndpointRunnableFactory} that collect statistics
 */
class TrackingEndpointRunnableFactory implements EndpointRunnableFactory {

  private final EndpointRunnableFactory delegate;
  private final IncomingCallsGatherer incomingCallsGatherer;
  private final RequestTracker requestTracker;

  TrackingEndpointRunnableFactory(
      EndpointRunnableFactory delegate,
      IncomingCallsGatherer incomingCallsGatherer,
      RequestTracker requestTracker) {
    this.delegate = delegate;
    this.incomingCallsGatherer = incomingCallsGatherer;
    this.requestTracker = requestTracker;
  }

  public Runnable create(
      OngoingRequest ongoingRequest,
      RequestContext requestContext,
      Endpoint endpoint) {

    incomingCallsGatherer.gatherIncomingCall(ongoingRequest, endpoint);

    final TrackedOngoingRequest trackedRequest =
        new TrackedOngoingRequestImpl(ongoingRequest, requestTracker);

    return delegate.create(trackedRequest, requestContext, endpoint);
  }
}
