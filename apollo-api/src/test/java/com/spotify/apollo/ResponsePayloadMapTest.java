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

import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

@RunWith(Theories.class)
public class ResponsePayloadMapTest {

  @DataPoints
  public static Response<String>[] responses() {
    Response<String> response = Response.<String>forStatus(Status.IM_A_TEAPOT)
        .withHeader("foo", "bar");

    //noinspection unchecked
    return new Response[] {
        response,
        response.withPayload("hello")

    };
  }

  @Theory
  public void mapsPayload(Response<String> input) throws Exception {
    Response<Integer> intResponse = input.mapPayload(String::length);

    if (input.payload().isPresent()) {
      assertThat(intResponse.payload().get().intValue(), is(5));
    } else {
      assertFalse(intResponse.payload().isPresent());
    }
  }

  @Theory
  public void preserveStatusAndHeaderOnMap(Response<String> input) throws Exception {
    Response<Integer> intResponse = input.mapPayload(String::length);

    assertThat(intResponse.status(), is(Status.IM_A_TEAPOT));
    assertThat(intResponse.headers(), hasEntry("foo", "bar"));
  }
}
