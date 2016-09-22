/*
 * -\-\-
 * Spotify Apollo API Implementations
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
package com.spotify.apollo.request;

import com.google.auto.value.AutoValue;

import com.spotify.apollo.RequestMetadata;

import java.util.Optional;

/**
 * Immutable value object for request metadata.
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@AutoValue
public abstract class RequestMetadataImpl implements RequestMetadata {

  public static RequestMetadata create(Class<?> sourceClass,
                                       long arrivalTimeNanos,
                                       String protocol,
                                       Optional<HostAndPort> localAddress,
                                       Optional<HostAndPort> remoteAddress) {
    return new AutoValue_RequestMetadataImpl(sourceClass, arrivalTimeNanos, protocol, localAddress, remoteAddress);
  }

  public static HostAndPort hostAndPort(String host, int port) {
    return new AutoValue_RequestMetadataImpl_HostAndPortImpl(host, port);
  }

  @AutoValue
  public static abstract class HostAndPortImpl implements RequestMetadata.HostAndPort {

  }
}
