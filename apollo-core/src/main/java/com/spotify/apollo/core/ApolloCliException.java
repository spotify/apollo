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
 * An exception related to command-line processing.  It is safe to display the exception's message
 * on the command-line.  {@link #getMessage()} returns the message to display.
 */
public class ApolloCliException extends ApolloException {

  public ApolloCliException(String message) {
    super(message);
  }

  public ApolloCliException(String message, Throwable cause) {
    super(message, cause);
  }
}
