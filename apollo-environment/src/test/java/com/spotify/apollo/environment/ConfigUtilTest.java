/*
 * -\-\-
 * Spotify Apollo API Environment
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
package com.spotify.apollo.environment;

import com.google.common.collect.ImmutableMap;

import org.junit.Test;

import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ConfigUtilTest {



  @Test
  public void optionalOrShouldReturnFirstIfPresent() throws Exception {
    assertThat(ConfigUtil.either(Optional.of("hi"), Optional.of("there")), is(Optional.of("hi")));
  }

  @Test
  public void optionalOrShouldReturnAlternativeIfFirstMissing() throws Exception {
    assertThat(ConfigUtil.either(Optional.empty(), Optional.of("there")), is(Optional.of("there")));
  }

//  @Test
//  public void shouldReturnValueForAvailableString() throws Exception {
//    final Config config = ConfigFactory.parseMap(ImmutableMap.of("hey", "ho"));
//    assertThat(ConfigUtil.optionalString(config, "hey"), is(Optional.of("ho")));
//  }
//
//  @Test
//  public void shouldReturnEmptyForMissingString() throws Exception {
//    final Config config = ConfigFactory.parseMap(ImmutableMap.of("hey", "ho"));
//    assertThat(ConfigUtil.optionalString(config, "ho"), is(Optional.empty()));
//  }
//
//  @Test
//  public void shouldReturnValueForAvailableBoolean() throws Exception {
//    final Config config = ConfigFactory.parseMap(ImmutableMap.of("hey", true));
//    assertThat(ConfigUtil.optionalBoolean(config, "hey"), is(Optional.of(true)));
//  }
//
//  @Test
//  public void shouldReturnEmptyForMissingBoolean() throws Exception {
//    final Config config = ConfigFactory.parseMap(ImmutableMap.of("hey", false));
//    assertThat(ConfigUtil.optionalBoolean(config, "ho"), is(Optional.empty()));
//  }
//
//  @Test
//  public void shouldReturnValueForAvailableInt() throws Exception {
//    final Config config = ConfigFactory.parseMap(ImmutableMap.of("hey", 345));
//    assertThat(ConfigUtil.optionalInt(config, "hey"), is(Optional.of(345)));
//  }
//
//  @Test
//  public void shouldReturnEmptyForMissingInt() throws Exception {
//    final Config config = ConfigFactory.parseMap(ImmutableMap.of("hey", 99));
//    assertThat(ConfigUtil.optionalInt(config, "ho"), is(Optional.empty()));
//  }
//
//  @Test
//  public void shouldReturnValueForAvailableDouble() throws Exception {
//    final Config config = ConfigFactory.parseMap(ImmutableMap.of("hey", 345.1));
//    assertThat(ConfigUtil.optionalDouble(config, "hey"), is(Optional.of(345.1)));
//  }
//
//  @Test
//  public void shouldReturnEmptyForMissingDouble() throws Exception {
//    final Config config = ConfigFactory.parseMap(ImmutableMap.of("hey", 99.0));
//    assertThat(ConfigUtil.optionalDouble(config, "ho"), is(Optional.empty()));
//  }
}