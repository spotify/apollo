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
package com.spotify.apollo.route;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MiddlewareTest {

  @Test
  public void shouldCompose() throws Exception {
    Middleware<String, Integer> a = s -> s.length();
    Middleware<Integer, Boolean> b = i -> i % 2 == 0;

    assertEquals(4, a.apply("four").intValue());
    assertTrue(b.apply(4));
    assertFalse(b.apply(5));

    Middleware<String, Boolean> composed = a.and(b);
    assertTrue(composed.apply("even"));
    assertTrue(composed.apply("evener"));
    assertFalse(composed.apply("odd"));
    assertFalse(composed.apply("odder"));
  }
}
