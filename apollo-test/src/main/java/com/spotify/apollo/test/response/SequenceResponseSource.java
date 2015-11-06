/*
 * Copyright (c) 2013-2014 Spotify AB
 */

package com.spotify.apollo.test.response;

import com.google.common.base.Preconditions;

import com.spotify.apollo.Request;

import java.util.Iterator;

class SequenceResponseSource implements ResponseSource {

  private final Iterator<ResponseWithDelay> responseWithDelayIterator;

  public SequenceResponseSource(Iterable<ResponseWithDelay> responseWithDelayIterable) {
    this.responseWithDelayIterator = responseWithDelayIterable.iterator();
  }

  @Override
  public ResponseWithDelay create(Request request) {
    Preconditions.checkState(responseWithDelayIterator.hasNext(), "No more responses specified!");
    return responseWithDelayIterator.next();
  }
}
