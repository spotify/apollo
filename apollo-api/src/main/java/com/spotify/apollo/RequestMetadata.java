/*-
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

import java.time.Instant;
import java.util.Optional;

/**
 * Describes an API for retrieving metadata about an incoming request. Implementations that wish to
 * enable tracking of additional metadata are expected to extend this interface with more methods to
 * return this additional data.
 */
public interface RequestMetadata {

  /**
   * Get the arrival time of the incoming request.
   */
  Instant arrivalTime();

  /**
   * Indicates the local address of the connection which the request was received on, if available
   */
  Optional<HostAndPort> localAddress();

  /**
   * Indicates the remote address of the connection which the request was received on, if available.
   * This may be the address of a proxy address or a direct connection to the caller, depending on
   * the network setup.
   */
  Optional<HostAndPort> remoteAddress();

  /**
   * Indicates the identity of the remote caller, if available.
   */
  default Optional<String> callerIdentity() {
    return Optional.empty();
  }

  /**
   * Defines an address
   */
  interface HostAndPort {

    /**
     * May be a hostname or an IP address.
     */
    String host();
    int port();
  }
}
