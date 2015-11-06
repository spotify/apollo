package com.spotify.apollo.environment;

import com.google.common.collect.ImmutableMap;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

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

  @Test
  public void shouldReturnValueForAvailableString() throws Exception {
    final Config config = ConfigFactory.parseMap(ImmutableMap.of("hey", "ho"));
    assertThat(ConfigUtil.optionalString(config, "hey"), is(Optional.of("ho")));
  }

  @Test
  public void shouldReturnEmptyForMissingString() throws Exception {
    final Config config = ConfigFactory.parseMap(ImmutableMap.of("hey", "ho"));
    assertThat(ConfigUtil.optionalString(config, "ho"), is(Optional.empty()));
  }

  @Test
  public void shouldReturnValueForAvailableBoolean() throws Exception {
    final Config config = ConfigFactory.parseMap(ImmutableMap.of("hey", true));
    assertThat(ConfigUtil.optionalBoolean(config, "hey"), is(Optional.of(true)));
  }

  @Test
  public void shouldReturnEmptyForMissingBoolean() throws Exception {
    final Config config = ConfigFactory.parseMap(ImmutableMap.of("hey", false));
    assertThat(ConfigUtil.optionalBoolean(config, "ho"), is(Optional.empty()));
  }

  @Test
  public void shouldReturnValueForAvailableInt() throws Exception {
    final Config config = ConfigFactory.parseMap(ImmutableMap.of("hey", 345));
    assertThat(ConfigUtil.optionalInt(config, "hey"), is(Optional.of(345)));
  }

  @Test
  public void shouldReturnEmptyForMissingInt() throws Exception {
    final Config config = ConfigFactory.parseMap(ImmutableMap.of("hey", 99));
    assertThat(ConfigUtil.optionalInt(config, "ho"), is(Optional.empty()));
  }

  @Test
  public void shouldReturnValueForAvailableDouble() throws Exception {
    final Config config = ConfigFactory.parseMap(ImmutableMap.of("hey", 345.1));
    assertThat(ConfigUtil.optionalDouble(config, "hey"), is(Optional.of(345.1)));
  }

  @Test
  public void shouldReturnEmptyForMissingDouble() throws Exception {
    final Config config = ConfigFactory.parseMap(ImmutableMap.of("hey", 99.0));
    assertThat(ConfigUtil.optionalDouble(config, "ho"), is(Optional.empty()));
  }
}