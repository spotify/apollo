/*
 * Copyright © 2014 Spotify AB
 */
package com.spotify.apollo.meta;

import com.spotify.apollo.dispatch.Endpoint;
import com.spotify.apollo.request.OngoingRequest;

public interface IncomingCallsGatherer {
  void gatherIncomingCall(OngoingRequest ongoingRequest, Endpoint endpoint);
}
