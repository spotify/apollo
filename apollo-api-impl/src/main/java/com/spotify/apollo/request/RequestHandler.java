/*
 * Copyright (c) 2013-2015 Spotify AB
 */

package com.spotify.apollo.request;

/**
 * A handler that will receive {@link OngoingRequest} objects that can be replied to.
 */
public interface RequestHandler {
  void handle(OngoingRequest ongoingRequest);
}
