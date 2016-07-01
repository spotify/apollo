/*
 * -\-\-
 * Spotify Apollo Metrics Module
 * --
 * Copyright (C) 2013 - 2016 Spotify AB
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
package com.spotify.apollo.metrics.semantic;

/**
 * Enumerates the metrics tracked, keyed by the 'what' tag.
 */
public enum What {
  REQUEST_FANOUT_FACTOR("request-fanout-factor"),
  ENDPOINT_REQUEST_RATE("endpoint-request-rate"),
  DROPPED_REQUEST_RATE("dropped-request-rate"),
  REQUEST_PAYLOAD_SIZE("request-payload-size"),
  RESPONSE_PAYLOAD_SIZE("response-payload-size"),
  ENDPOINT_REQUEST_DURATION("endpoint-request-duration"),
  ERROR_RATIO("error-ratio")
  ;

  private final String tag;

  What(String tag) {
    this.tag = tag;
  }

  public String tag() {
    return tag;
  }
}
