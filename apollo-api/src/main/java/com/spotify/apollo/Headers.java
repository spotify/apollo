/*
 * -\-\-
 * Spotify Apollo API Interfaces
 * --
 * Copyright (C) 2013 - 2017 Spotify AB
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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@AutoValue
abstract class Headers {

  static Headers EMPTY = create(Collections.emptyMap());

  static Headers create(Map<String, String> headers) {
    final List<Map.Entry<String, String>> headersList = new ArrayList<>(headers.size());
    headers.entrySet().forEach(h -> insertOrReplace(headersList, h));
    return new AutoValue_Headers(ImmutableList.copyOf(headersList));
  }

  public Optional<String> get(String name) {
    Objects.requireNonNull(name, "Header names cannot be null");

    for (int i = 0; i < entries().size(); i++) {
      final Map.Entry<String, String> headerEntry = entries().get(i);
      if (name.equalsIgnoreCase(headerEntry.getKey())) {
        return Optional.ofNullable(headerEntry.getValue());
      }
    }

    return Optional.empty();
  }

  public Map<String, String> asMap() {
    ImmutableMap.Builder<String, String> headers = ImmutableMap.builder();
    entries().forEach(headers::put);
    return headers.build();
  }

  public abstract ImmutableList<Map.Entry<String, String>> entries();

  private static void insertOrReplace(List<Map.Entry<String, String>> headerList,
                                      Map.Entry<String, String> newHeader) {

    // Replace existing header with new key (letter case can be overwritten) and value
    for (int i = 0; i < headerList.size(); i++) {
      Map.Entry<String, String> currentHeader = headerList.get(i);
      if (currentHeader.getKey().equalsIgnoreCase(newHeader.getKey())) {
        headerList.set(i, new SimpleImmutableEntry<>(newHeader));
        return;
      }
    }

    // No matching entry present, add new entry
    headerList.add(new SimpleImmutableEntry<>(newHeader));
  }

}
