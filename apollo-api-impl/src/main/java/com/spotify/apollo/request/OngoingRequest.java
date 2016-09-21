/*
 * -\-\-
 * Spotify Apollo API Implementations
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
package com.spotify.apollo.request;

import com.spotify.apollo.Request;
import com.spotify.apollo.RequestMetadata;
import com.spotify.apollo.Response;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.Optional;

import okio.ByteString;

/**
 * A request that is being processed.
 */
public interface OngoingRequest {

  InetSocketAddress PORT_ZERO = new InetSocketAddress(0);
  ServerInfo UNKNOWN_SERVER_INFO = ServerInfos.create("unknown", PORT_ZERO);

  /**
   * Returns the {@link Request}.
   */
  Request request();

  /**
   * Returns an identifier for the server where this request originated.
   */
  default ServerInfo serverInfo() {
    return UNKNOWN_SERVER_INFO;
  }

  /**
   * Reply to the request with a {@link Response}.
   *
   * @param response  Response to send as reply
   */
  void reply(Response<ByteString> response);

  /**
   * Drop the request.
   */
  void drop();

  boolean isExpired();

  /**
   * Get the arrival time of the incoming request in nanoseconds. Note that this is not
   * unix epoch as the time is provided by {@link System#nanoTime()}. To get unix epoch
   * time, do something like:
   *
   * <pre>
   * {@code
   * long processingTimeNanos = System.nanoTime() - requestContext.arrivalTimeNanos();
   * long arrivalTimeUnixEpochMillis = System.currentTimeMillis() +
   *                                   TimeUnit.NANOSECONDS.toMillis(processingTimeNanos);
   * }
   * </pre>
   *
   * @see System#nanoTime()
   */
  default long arrivalTimeNanos() {
    // This is not a good default for real implementations. It is simply a catch-all
    // default to not break existing implementations.
    return System.nanoTime();
  }

  /**
   * Returns the metadata available for this request.
   */
  default RequestMetadata metadata() {
    return RequestMetadataImpl.create(Instant.now(), Optional.empty(), Optional.empty());
  }
}
