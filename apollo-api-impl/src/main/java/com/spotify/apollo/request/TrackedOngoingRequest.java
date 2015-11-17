/*
 * Copyright (c) 2013-2015 Spotify AB
 */

package com.spotify.apollo.request;

/**
 * A {@link OngoingRequest} that is being tracked throughout the request handling process.
 */
public interface TrackedOngoingRequest extends OngoingRequest {

  /**
   * This should be called for each downstream request to other services that is triggered by this
   * request's endpoint handler.
   */
  void incrementDownstreamRequests();
}
