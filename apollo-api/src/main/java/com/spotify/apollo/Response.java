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

import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

/**
 * Defines the data Apollo needs to construct and send a response to an incoming request.
 *
 * Response instances are immutable. However, headers can be added using
 * {@link #withHeader(String, String)} and {@link #withHeaders(Map)} methods, and a payload set via
 * {@link #withPayload(Object)}. These methods will create new copies of the {@link Response} object.
 */
public interface Response<T> {

  /**
   * The status of the response message.
   */
  StatusType status();

  /**
   * The response headers.
   */
  Headers headers();

  /**
   * The single payload of the response.
   */
  Optional<T> payload();

  /**
   * Creates a new {@link Response} based on this, but with an additional header.
   *
   * @param header  Header name to add
   * @param value   Header value
   * @return A response with the added header
   */
  Response<T> withHeader(String header, String value);

  /**
   * Creates a new {@link Response} based on this, but with additional headers.
   *
   * @param headers  Map of header name-values to add
   * @return A response with the added headers
   */
  Response<T> withHeaders(Map<String, String> headers);

  /**
   * Creates a new {@link Response} based on this, but with a different payload. Status code,
   * headers, etc., are copied over. To clear out the payload, one can pass in {@code null}.
   *
   * @param newPayload the new payload
   */
  <P> Response<P> withPayload(@Nullable P newPayload);

  /**
   * Returns a typed 200 OK {@link Response}.
   *
   * @param <T>  The response payload type
   * @return A response object
   */
  static <T> Response<T> ok() {
    //noinspection unchecked
    return (Response<T>) ResponseImpl.OK;
  }

  /**
   * Creates a {@link Response} with the given status code.
   *
   * If {@code code} is OK, this function will be equivalent to {@link #ok()}.
   *
   * @param statusCode  The status code
   * @param <T>         The response payload type
   * @return A response object with the given status code
   */
  static <T> Response<T> forStatus(StatusType statusCode) {
    return ResponseImpl.create(statusCode);
  }

  /**
   * Creates a 200 OK {@link Response} with the given payload of type {@link T} .
   *
   * @param payload  The payload to respond with
   * @param <T>      The response payload type
   * @return A response object with the given payload
   */
  static <T> Response<T> forPayload(T payload) {
    return ResponseImpl.create(payload);
  }

  /**
   * Creates a {@link Response} with the given status code and a payload with type {@link T}.
   *
   * @param statusCode  The status code
   * @param payload     The payload to respond with
   * @param <T>         The response payload type
   * @return A response object with the given status code and payload
   */
  static <T> Response<T> of(StatusType statusCode, T payload) {
    return ResponseImpl.create(statusCode, payload);
  }
}
