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

  /**
   * Fails this request because of some exceptional cause. This should only be called if the normal
   * endpoint handling flow is not able to execute properly.
   *
   * @param cause  The cause for failing this request
   */
  void fail(FailureCause cause);

  enum FailureCause {
    EXCEPTION,
    TIMEOUT
  }
}
