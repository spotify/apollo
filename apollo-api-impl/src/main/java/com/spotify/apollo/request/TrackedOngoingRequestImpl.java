/*
 * Copyright (c) 2013 Spotify AB
 */

package com.spotify.apollo.request;

import com.spotify.apollo.Response;

import okio.ByteString;

class TrackedOngoingRequestImpl
    extends ForwardingOngoingRequest
    implements TrackedOngoingRequest {

  private final RequestTracker requestTracker;

  TrackedOngoingRequestImpl(
      OngoingRequest ongoingRequest,
      RequestTracker requestTracker) {
    super(ongoingRequest);
    this.requestTracker = requestTracker;

    requestTracker.register(this);
  }

  @Override
  public void reply(Response<ByteString> message) {
    doReply(message);
  }

  @Override
  public void drop() {
    final boolean removed = requestTracker.remove(this);
    if (removed) {
      super.drop();
    }
  }

  @Override
  public void incrementDownstreamRequests() {
  }

  private boolean doReply(Response<ByteString> message) {
    final boolean removed = requestTracker.remove(this);
    if (removed) {
      super.reply(message);
    }
    // TODO: how to log this?
//    else {
//      logRequest("DROPPED");
//    }
    return removed;
  }
}
