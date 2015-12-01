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

import java.util.List;
import java.util.Map;
import java.util.Optional;

import okio.ByteString;

/**
 * Defines the data Apollo presents for an incoming request.
 *
 * Request instances are immutable. However, requests can be built using
 * {@link #forUri(String, String)} methods, and headers and payload can
 * be added using {@link #withHeader(String, String)} and {@link #withPayload(ByteString)}.
 */
public interface Request {
  /**
   * The method of the request message.
   */
  String method();

  /**
   * The uri of the request message.
   * Uri query parameters are available via {@link #parameter(String)} and {@link #parameters()}.
   * Uri pathArgs are available in {@link RequestContext}.
   */
  String uri();

  /**
   * The uri query parameters of the request message.
   */
  Map<String, List<String>> parameters();

  /**
   * A uri query parameter of the request message, or empty if no parameter with that name is found.
   * Returns the first query parameter value if it is repeated. Use {@link #parameters()} to get
   * all repeated values.
   */
  default Optional<String> parameter(String parameter) {
    List<String> values = parameters().get(parameter);
    if (values != null) {
      return Optional.ofNullable(values.get(0));
    } else {
      return Optional.empty();
    }
  }

  /**
   * The headers of the request message.
   */
  Map<String, String> headers();

  /**
   * A header of the request message, or empty if no header with that name is found.
   */
  default Optional<String> header(String name) {
    return Optional.ofNullable(headers().get(name));
  }

  /**
   * The calling service of the request, if known.
   */
  Optional<String> service();

  /**
   * The request message payload, or empty if there is no payload.
   */
  Optional<ByteString> payload();

  /**
   * Creates a new {@link Request} based on this, but with a different calling service.
   *
   * @param service the new service
   */
  Request withService(String service);

  /**
   * Creates a new {@link Response} based on this, but with an additional header.
   *
   * @param name  Header name to add
   * @param value  Header value
   * @return A request with the added header
   */
  Request withHeader(String name, String value);

  /**
   * Creates a new {@link Request} based on this, but with a different payload.
   *
   * @param payload the new payload
   */
  Request withPayload(ByteString payload);

  /**
   * Creates a {@link Request} for the given uri and method.
   *
   * @param uri  The requested uri
   * @param method  The request method
   * @return A request with for the given uri and method.
   */
  static Request forUri(String uri, String method) {
    return RequestValue.create(uri, method);
  }

  /**
   * Creates a {@link Request} for the given uri.
   *
   * @param uri  The requested uri
   * @return A request with for the given uri.
   */
  static Request forUri(String uri) {
    return RequestValue.create(uri);
  }
}
