/*
 * Copyright (c) 2013-2015 Spotify AB
 */

package com.spotify.apollo.request;

import com.spotify.apollo.dispatch.Endpoint;
import com.spotify.apollo.route.RuleMatch;

import java.util.function.BiConsumer;

/**
 * A runner for request that will handle matching the request against an {@link Endpoint}.
 */
@FunctionalInterface
public interface RequestRunnable {

  /**
   * Do the matching and hand over the match to the given continuation {@link BiConsumer}.
   *
   * @param matchContinuation  The continuation that should process the match
   */
  void run(BiConsumer<OngoingRequest, RuleMatch<Endpoint>> matchContinuation);
}
