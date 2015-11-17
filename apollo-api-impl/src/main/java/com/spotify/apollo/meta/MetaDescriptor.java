/*
 * Copyright (c) 2014 Spotify AB
 */

package com.spotify.apollo.meta;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public class MetaDescriptor {

  private static final Logger LOG = LoggerFactory.getLogger(MetaDescriptor.class);

  private static final String APOLLO_VERSION = "X-Spotify-Apollo-Version";
  private static final String IMPL_VERSION = "Implementation-Version";

  private final Descriptor descriptor;
  private final String apolloVersion;

  MetaDescriptor(Descriptor descriptor, String apolloVersion) {
    this.descriptor = descriptor;
    this.apolloVersion = apolloVersion;
  }

  public Descriptor descriptor() {
    return descriptor;
  }

  public String apolloVersion() {
    return apolloVersion;
  }

  // TODO: load real artifact and group id
  // TODO: can apollo version be loaded more reliably?
  public static MetaDescriptor readMetaDescriptor(String serviceName, ClassLoader classLoader)
      throws IOException {
    String apolloVersion;
    String version;

    try {
      apolloVersion = loadApolloVersion(classLoader);
    } catch (IOException e) {
      apolloVersion = "0.0.0-UNKNOWN";
    }

    version = loadVersion();
    version = version != null ? version : "0.0.0-UNKNOWN";

    return new MetaDescriptor(
        Descriptor.create(serviceName, version),
        apolloVersion);
  }

  protected static String loadApolloVersion(ClassLoader classLoader) throws IOException {
    Properties properties = new Properties();
    properties.load(classLoader.getResourceAsStream("metaDescriptor.properties"));
    return properties.getProperty("apolloVersion");
  }

  protected static String loadVersion() {
    return MetaDescriptor.class.getPackage().getImplementationVersion();
  }
}
