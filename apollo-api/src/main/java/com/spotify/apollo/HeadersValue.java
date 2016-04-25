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

import com.google.auto.value.AutoValue;
import com.google.common.base.CharMatcher;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import javax.annotation.Nonnull;

/**
 * Ensures that the lower-case contract of {@link Headers} is met by the value objects.
 */
@AutoValue
abstract class HeadersValue implements Headers {

  private static final CharMatcher ASCII_UPPER_CASE = CharMatcher.inRange('A', 'Z');

  @Nonnull
  @Override
  public Optional<String> get(@Nonnull String name) {
    return Optional.ofNullable(asMap().get(name.toLowerCase()));
  }

  static Headers create(Map<String, String> headers) {
    Map<String, String> allLowerCaseKeys = headers.keySet().stream()
        .filter(HeadersValue::hasNonLowerCase)
        .findAny()
        .map(lowerCaseKeys(headers))
        .orElse(headers);

    return new AutoValue_HeadersValue(allLowerCaseKeys);
  }

  private static Function<String, Map<String, String>> lowerCaseKeys(Map<String, String> headers) {
    return s -> {
      Map<String, String> result = new LinkedHashMap<>(headers.size());

      for (Map.Entry<String, String> entry : headers.entrySet()) {
        result.put(entry.getKey().toLowerCase(), entry.getValue());
      }

      return result;
    };
  }

  private static boolean hasNonLowerCase(String s) {
    for (char c : s.toCharArray()) {
      if (ASCII_UPPER_CASE.matches(c)) {
        return true;
      }
    }

    return false;
  }
}
