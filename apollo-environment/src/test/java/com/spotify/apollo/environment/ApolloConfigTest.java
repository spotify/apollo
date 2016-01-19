package com.spotify.apollo.environment;

import com.google.common.collect.ImmutableMap;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class ApolloConfigTest {

  @Test
  public void testBackendApolloBackend() throws Exception {
    final Config config = ConfigFactory.parseMap(ImmutableMap.of(
        "apollo.backend", "foo"
    ));
    final ApolloConfig sut = new ApolloConfig(config);

    assertThat(sut.backend(), is("foo"));
  }

  @Test
  public void testBackendApolloDomain() throws Exception {
    final Config config = ConfigFactory.parseMap(ImmutableMap.of(
        "apollo.domain", "foo"
    ));
    final ApolloConfig sut = new ApolloConfig(config);

    assertThat(sut.backend(), is("foo"));
  }

  @Test
  public void testBackendDomain() throws Exception {
    final Config config = ConfigFactory.parseMap(ImmutableMap.of(
        "domain", "foo"
    ));
    final ApolloConfig sut = new ApolloConfig(config);

    assertThat(sut.backend(), is("foo"));
  }

  @Test
  public void testBackendApolloDefault() throws Exception {
    final Config config = ConfigFactory.parseMap(ImmutableMap.of());
    final ApolloConfig sut = new ApolloConfig(config);

    assertThat(sut.backend(), is(""));
  }

  @Test
  public void testEnableIncomingRequestLogging() throws Exception {
    final Config config = ConfigFactory.parseMap(ImmutableMap.of(
        "apollo.logIncomingRequests", false
    ));
    final ApolloConfig sut = new ApolloConfig(config);

    assertThat(sut.enableIncomingRequestLogging(), is(false));
  }

  @Test
  public void testEnableIncomingRequestLoggingDefault() throws Exception {
    final Config config = ConfigFactory.parseMap(ImmutableMap.of());
    final ApolloConfig sut = new ApolloConfig(config);

    assertThat(sut.enableIncomingRequestLogging(), is(true));
  }

  @Test
  public void testEnableMetaApi() throws Exception {
    final Config config = ConfigFactory.parseMap(ImmutableMap.of(
        "apollo.metaApi", true
    ));
    final ApolloConfig sut = new ApolloConfig(config);

    assertThat(sut.enableMetaApi(), is(true));
  }

  @Test
  public void testEnableMetaApiDefault() throws Exception {
    final Config config = ConfigFactory.parseMap(ImmutableMap.of());
    final ApolloConfig sut = new ApolloConfig(config);

    assertThat(sut.enableMetaApi(), is(true));
  }
}
