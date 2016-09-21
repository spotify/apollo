/*
 * -\-\-
 * Spotify Apollo API Interfaces
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
package com.spotify.apollo;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

/**
 * Describes an API for retrieving metadata about an incoming request.
 */
public interface RequestMetadata {

  String METADATA_SOURCE = "apollo.metadata-source";

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
   * Returns metadata for this request; may include things like information about the origin of the
   * request (remote IP address of the connected socket), etc. The exact content is server
   * implementation-dependent. Implementations are encouraged to clearly document which set of keys
   * and values they will record as request metadata. All implementations are encouraged to set
   * a value for the key {@link #METADATA_SOURCE}, indicating a Java class that generated the
   * metadata; this enables users to more easily understand what data to expect.
   */
  default Map<String, String> metadata() {
    return ImmutableMap.of();
  }
}
