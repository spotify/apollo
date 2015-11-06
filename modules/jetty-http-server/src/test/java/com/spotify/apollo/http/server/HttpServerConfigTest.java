/*
 * Copyright (c) 2014 Spotify AB
 */

package com.spotify.apollo.http.server;

import com.typesafe.config.ConfigFactory;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class HttpServerConfigTest {

  private static final String SERVICE_NAME = "http-test-service-name";

  @Test
  public void canConfigureAddressAndPort() {
    String addr = "someaddr";
    int port = 1234;
    String json =
        String.format("{\"http\":{\"server\":{\"address\":\"%s\", \"port\":%d}}}", addr, port);

    HttpServerConfig http = conf(json);
    assertEquals(addr, http.address());
    assertEquals(port, http.port().intValue());
  }

  @Test
  public void canConfigureTtlMillis() {
    long ttlMillis = 123L;
    String json = "{\"http\":{\"server\":{\"ttlMillis\": 123}}}";

    HttpServerConfig http = conf(json);
    assertEquals(ttlMillis, http.ttlMillis());
  }

  @Test
  public void canConfigureWorkerThreads() {
    long workerThreads = 123L;
    String json = "{\"http\":{\"server\":{\"workerThreads\": 123}}}";

    HttpServerConfig http = conf(json);
    assertEquals(workerThreads, http.workerThreads());
  }

  @Test
  public void canConfigureMaxHttpChunkLength() {
    long maxHttpChunkLength = 123L;
    String json = "{\"http\":{\"server\":{\"maxHttpChunkLength\": 123}}}";

    HttpServerConfig http = conf(json);
    assertEquals(maxHttpChunkLength, http.maxHttpChunkLength());
  }

  @Test
  public void canConfigureKeepAliveTimeout() {
    long keepAliveTimeout = 123L;
    String json = "{\"http\":{\"server\":{\"keepAliveTimeout\": 123}}}";

    HttpServerConfig http = conf(json);
    assertEquals(keepAliveTimeout, http.keepAliveTimeout());
  }

  @Test
  public void hasNoRegistrationNameByDefault() {
    String json = "{\"http\":{\"server\":{}}}";

    HttpServerConfig http = conf(json);
    assertEquals(SERVICE_NAME, http.registrationName());
  }

  @Test
  public void canConfigureRegistrationName() {
    String json = "{\"http\":{\"server\":{\"registrationName\": \"foobar\"}}}";

    HttpServerConfig http = conf(json);
    assertEquals("foobar", http.registrationName());
  }

  private static HttpServerConfig conf(String json) {
    return new HttpServerConfig(SERVICE_NAME, ConfigFactory.parseString(json));
  }
}
