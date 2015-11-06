/*
 * Copyright (c) 2013-2014 Spotify AB
 */

package com.spotify.apollo.test.response;

import com.spotify.apollo.Request;

class ConstantResponseSource implements ResponseSource {

  private final ResponseWithDelay responseWithDelay;

  public ConstantResponseSource(ResponseWithDelay responseWithDelay) {
    this.responseWithDelay = responseWithDelay;
  }

  @Override
  public ResponseWithDelay create(Request request) {
    return responseWithDelay;
  }
}
