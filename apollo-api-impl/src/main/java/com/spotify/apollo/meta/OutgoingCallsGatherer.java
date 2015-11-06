/*
 * Copyright © 2014 Spotify AB
 */
package com.spotify.apollo.meta;

import com.spotify.apollo.Request;

public interface OutgoingCallsGatherer {
  void gatherOutgoingCall(String toService, Request request);
}
