/*
 * -\-\-
 * Spotify Apollo API Interfaces
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
package com.spotify.apollo;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * This object contains all the needed information related to an incoming request.
 */
public interface RequestContext {

  /**
   * Get the incoming request message.
   *
   * @return the request message
   */
  Request request();

  /**
   * Get an Apollo client that can be used to make backend service requests. The requests will have
   * the auth context of the incoming request applied.
   *
   * For a non-scoped Client, see {@link Environment#client()}.
   *
   * @return A {@link Client}.
   */
  Client requestScopedClient();

  /**
   * Gets a map of parsed path arguments. If Route is defined as having the path
   * "/somewhere/<param>/<param2:path>", and it gets invoked with a URI with the path
   * "/somewhere/over/the%32rainbow", then the map will be
   * { "param" : "over", "param2": "the%32rainbow" }.
   */
  Map<String, String> pathArgs();

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
    return new RequestMetadata() {
      @Override
      public Instant arrivalTime() {
        return Instant.now();
      }

      @Override
      public Optional<HostAndPort> localAddress() {
        return Optional.empty();
      }

      @Override
      public Optional<HostAndPort> remoteAddress() {
        return Optional.empty();
      }
    };
  }
}
