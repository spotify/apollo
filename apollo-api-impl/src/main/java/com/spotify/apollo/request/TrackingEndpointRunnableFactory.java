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
