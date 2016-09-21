/*
 * -\-\-
 * Spotify Apollo Jetty HTTP Server Module
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
package com.spotify.apollo.http.server;

import com.google.auto.value.AutoValue;

import com.spotify.apollo.RequestMetadata;

import java.time.Instant;
import java.util.Optional;

/**
 * Subclass adding HTTP-specific metadata for incoming requests.
 */
@AutoValue
public abstract class HttpRequestMetadata implements RequestMetadata {
  /**
   * The value of the HTTP-Version in the incoming HTTP Request-Line
   * as per https://www.w3.org/Protocols/rfc2616/rfc2616-sec5.html. That is, usually one of
   * HTTP/1.0 and HTTP/1.1.
   */
  public abstract String httpVersion();

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  static HttpRequestMetadata create(
      Instant arrivalTime,
      Optional<HostAndPort> localAddress,
      Optional<HostAndPort> remoteAddress,
      String httpVersion) {
    return new AutoValue_HttpRequestMetadata(arrivalTime, localAddress, remoteAddress, httpVersion);
  }
}
