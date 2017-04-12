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

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class HeadersTest {

  private static final Map<String, String> TEST_MAP_DUPLICATE_KEYS = createDuplicateKeysMap();
  private static final Map<String, String> TEST_BIG_MAP = createBigMap();

  private static Map<String, String> createDuplicateKeysMap() {
    LinkedHashMap<String, String> testMap = new LinkedHashMap<>(3);
    testMap.put("first-key", "value1");
    testMap.put("second-key", "other-value");
    testMap.put("FirST-KEy", "value2");
    return testMap;
  }

  private static Map<String, String> createBigMap() {
    LinkedHashMap<String, String> testMap = new LinkedHashMap<>(100);
    for (int i = 0; i < 100; i++) {
      String number = String.valueOf(i);
      testMap.put(number, number);
    }
    return testMap;
  }

  @Test
  public void testGetIsCaseInsensitiveLower() {
    Map<String, String> map = Collections.singletonMap("x-name", "value");
    Headers headers = Headers.create(map);

    assertThat(headers.get("X-NaMe"), is(Optional.of("value")));
  }

  @Test
  public void testGetIsCaseInsensitiveUpper() {
    Map<String, String> map = Collections.singletonMap("X-NAme", "value");
    Headers headers = Headers.create(map);

    assertThat(headers.get("x-name"), is(Optional.of("value")));
  }

  @Test
  public void testAsMapPreservesOrderForOverwrittenKeys() {
    Headers headers = Headers.create(TEST_MAP_DUPLICATE_KEYS);
    ArrayList<Map.Entry<String, String>> asMapResult = new ArrayList<>(headers.asMap().entrySet());

    assertThat(headers.asMap().size(), is(2));

    // First key and value where overwritten, but kept on the same first position
    assertThat(asMapResult.get(0).getKey(), is("FirST-KEy"));
    assertThat(asMapResult.get(0).getValue(), is("value2"));

    // Second header key and value stayed on the second position
    assertThat(asMapResult.get(1).getKey(), is("second-key"));
    assertThat(asMapResult.get(1).getValue(), is("other-value"));
  }

  @Test
  public void testAsMapPreservesOrderFromConstructor() {
    Headers headers = Headers.create(TEST_BIG_MAP);

    ArrayList<Map.Entry<String, String>> asMapResult = new ArrayList<>(headers.asMap().entrySet());
    assertThat(asMapResult.size(), is(TEST_BIG_MAP.size()));

    for (int i = 0; i < asMapResult.size(); i++) {
      Map.Entry<String, String> header = asMapResult.get(i);
      assertThat(header.getKey(), is(String.valueOf(i)));
    }
  }

  @Test
  public void testAsMapPreservesLetterCase() {
    Map<String, String> map = Collections.singletonMap("sTRangE-KEy", "value");
    Headers headers = Headers.create(map);

    Map.Entry<String, String> asMapResult = headers.asMap().entrySet().iterator().next();

    assertThat(asMapResult.getKey(), is("sTRangE-KEy"));
  }

  @Test
  public void testEntriesPreservesOrderForOverwrittenKeys() {
    Headers headers = Headers.create(TEST_MAP_DUPLICATE_KEYS);

    ArrayList<Map.Entry<String, String>> entriesResult = new ArrayList<>(headers.entries());

    assertThat(headers.entries().size(), is(2));

    // First key and value where overwritten, but kept on the same first position
    assertThat(entriesResult.get(0).getKey(), is("FirST-KEy"));
    assertThat(entriesResult.get(0).getValue(), is("value2"));

    // Second header key and value stayed on the second position
    assertThat(entriesResult.get(1).getKey(), is("second-key"));
    assertThat(entriesResult.get(1).getValue(), is("other-value"));
  }

  @Test
  public void testEntriesPreservesOrderFromConstructor() {
    Headers headers = Headers.create(TEST_BIG_MAP);

    ArrayList<Map.Entry<String, String>> entries = new ArrayList<>(headers.entries());
    assertThat(entries.size(), is(TEST_BIG_MAP.size()));

    for (int i = 0; i < entries.size(); i++) {
      Map.Entry<String, String> header = entries.get(i);
      assertThat(header.getKey(), is(String.valueOf(i)));
    }
  }

  @Test
  public void testEntriesPreservesLetterCase() {
    Map<String, String> map = Collections.singletonMap("sTRangE-KEy", "value");
    Headers headers = Headers.create(map);

    Map.Entry<String, String> asMapResult = headers.entries().get(0);

    assertThat(headers.entries().size(), is(1));
    assertThat(asMapResult.getKey(), is("sTRangE-KEy"));
  }

  @Test
  public void testEmptyConstructor() {
    Headers headers = Headers.create(Collections.emptyMap());
    assertThat(headers.asMap().size(), is(0));
    assertThat(headers.get("non-existent"), is(Optional.empty()));
  }
}
