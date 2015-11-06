/*
 * Copyright (c) 2013-2014 Spotify AB
 */

package com.spotify.apollo.test.response;

public final class Responses {

  private Responses() {
  }

  public static ResponseSource constant(final ResponseWithDelay responseWithDelay) {
    return new ConstantResponseSource(responseWithDelay);
  }

  public static ResponseSource sequence(final Iterable<ResponseWithDelay> responseWithDelayIterable) {
    return new SequenceResponseSource(responseWithDelayIterable);
  }
}
