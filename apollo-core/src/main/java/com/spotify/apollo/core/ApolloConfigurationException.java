/*
 * -\-\-
 * Spotify Apollo Service Core (aka Leto)
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
package com.spotify.apollo.core;

/**
 * Thrown if the Apollo configuration is invalid due to a programmer error, e.g. asking for an
 * instance of an unloaded module. Clients should stop execution if this exception occurs.
 */
public class ApolloConfigurationException extends RuntimeException {

  public ApolloConfigurationException(String message, Throwable cause) {
    super(message, cause);
  }

}
