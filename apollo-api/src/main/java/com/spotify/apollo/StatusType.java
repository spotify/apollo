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

/**
 * Definition of a type for HTTP status codes.
 */
public interface StatusType {

  int code();
  String reasonPhrase();
  Family family();

  /**
   * Returns a StatusType instance with the same code value as the current instance, but
   * using the supplied reasonPhrase. The returned instance may be the same as object the method
   * is invoked on, or different.
   *
   * @param reasonPhrase the reason phrase to use
   * @return a possibly different object with the same status code and the supplied reasonPhrase
   */
  StatusType withReasonPhrase(String reasonPhrase);

  /**
   * Defines classes of status codes as described in
   * http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html, using the name Family since 'class'
   * is overloaded in Java.
   */
  // stolen with pride from javax.ws.rs.core
  enum Family {
    /**
     * {@code 1xx} HTTP status codes.
     */
    INFORMATIONAL,
    /**
     * {@code 2xx} HTTP status codes.
     */
    SUCCESSFUL,
    /**
     * {@code 3xx} HTTP status codes.
     */
    REDIRECTION,
    /**
     * {@code 4xx} HTTP status codes.
     */
    CLIENT_ERROR,
    /**
     * {@code 5xx} HTTP status codes.
     */
    SERVER_ERROR,
    /**
     * Other, unrecognized HTTP status codes.
     */
    OTHER;

    /**
     * Get the response status family for the status code.
     *
     * @param statusCode response status code to get the family for.
     * @return family of the response status code.
     */
    public static Family familyOf(final int statusCode) {
      switch (statusCode / 100) {
        case 1:
          return INFORMATIONAL;
        case 2:
          return SUCCESSFUL;
        case 3:
          return REDIRECTION;
        case 4:
          return CLIENT_ERROR;
        case 5:
          return SERVER_ERROR;
        default:
          return OTHER;
      }
    }

    public String rangeName() {
      switch (this) {
        case INFORMATIONAL:
          return "1xx";
        case SUCCESSFUL:
          return "2xx";
        case REDIRECTION:
          return "3xx";
        case CLIENT_ERROR:
          return "4xx";
        case SERVER_ERROR:
          return "5xx";
        default:
          return "unknown";
      }
    }
  }
}
