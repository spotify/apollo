/*
 * -\-\-
 * Spotify Apollo Testing Helpers
 * --
 * Copyright (C) 2013 - 2015 Spotify AB
 * --
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -/-/-
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
