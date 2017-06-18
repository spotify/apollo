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
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nonnull;

/**
 * Immutable headers API.
 */
public interface Headers {

  Headers EMPTY = of(Collections.emptyMap());

  /**
   * Returns the value for the supplied header name, via a case-insensitive comparison. For a multi-
   * valued HTTP header, this will return the combined form described in
   * https://tools.ietf.org/html/rfc7230#section-3.2.2 (multiple values are combined into a single
   * comma-separated list).
   *
   * @param name a header name
   * @return the value for the header, or empty, if missing
   */
  @Nonnull
  Optional<String> get(@Nonnull String name);

  /**
   * Returns an immutable map with keys that are the lower-case header names, and values the
   * corresponding header values.
   *
   * @return the contents of this object as a map
   */
  @Nonnull
  ImmutableMap<String, String> asMap();

  @Nonnull
  static Headers of(Map<String, String> headers) {
    return HeadersValue.create(headers);
  }
}
