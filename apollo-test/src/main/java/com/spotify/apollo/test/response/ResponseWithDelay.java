/*
 * Copyright (c) 2013-2014 Spotify AB
 */

package com.spotify.apollo.test.response;

import com.spotify.apollo.Response;

import java.util.concurrent.TimeUnit;

import okio.ByteString;

public class ResponseWithDelay {

  private final Response<ByteString> response;
  private final long delayMillis;

  public static ResponseWithDelay forResponse(Response<ByteString> response) {
    return new ResponseWithDelay(response, 0);
  }

  public static ResponseWithDelay forResponse(
      Response<ByteString> response, long delay, TimeUnit unit) {
    return new ResponseWithDelay(response, unit.toMillis(delay));
  }

  private ResponseWithDelay(Response<ByteString> response, long delayMillis) {
    this.response = response;
    this.delayMillis = delayMillis;
  }

  public Response<ByteString> getResponse() {
    return response;
  }

  public long getDelayMillis() {
    return delayMillis;
  }
}
