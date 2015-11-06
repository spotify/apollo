/*
 * Copyright (c) 2013-2014 Spotify AB
 */

package com.spotify.apollo.test.response;

import com.spotify.apollo.Request;

public interface ResponseSource {

  ResponseWithDelay create(Request request);
}
