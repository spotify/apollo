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

import java.time.Instant;
import java.util.Optional;

/**
 * Describes an API for retrieving metadata about an incoming request.
 */
public interface RequestMetadata {

  /**
   * Returns the class that is the source of the metadata; this enables users to more easily
   * understand where the metadata comes from and how to interpret data like the {@link #protocol()}.
   */
  Class<?> source();

  /**
   * Get the arrival time of the incoming request.
   */
  Instant arrivalTime();

  /**
   * A description of the protocol used in the incoming request. How this description is derived
   * is implementation-dependent; the intention is to use it to return information like the
   * HTTP-Version in an HTTP Request-Line (https://www.w3.org/Protocols/rfc2616/rfc2616-sec5.html).
   */
  String protocol();

  /**
   * Indicates the local address of the connection which the request was received on.
   */
  Optional<HostAndPort> localAddress();

  /**
   * Indicates the remote address of the connection which the request was received on. This may be
   * the address of a proxy address or a direct connection to the caller, depending on the network
   * setup.
   */
  Optional<HostAndPort> remoteAddress();

  /**
   * For defining addresses
   */
  interface HostAndPort {
    String host();
    int port();
  }
}
