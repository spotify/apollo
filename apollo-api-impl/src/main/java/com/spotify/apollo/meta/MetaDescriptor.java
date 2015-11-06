/*
 * Copyright (c) 2014 Spotify AB
 */

package com.spotify.apollo.meta;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
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
    final String apolloVersion;
    final String version;
    final Manifest manifest = getManifest(classLoader);

    if (manifest != null) {
      final Attributes attributes = manifest.getMainAttributes();
      apolloVersion = attributes.getValue(APOLLO_VERSION);
      version = attributes.getValue(IMPL_VERSION);
    } else {
      LOG.warn("Could not find manifest, continuing with default artifact metadata");

      apolloVersion = "0.0.0-UNKNOWN";
      version = "0.0.0-UNKNOWN";
    }

    return new MetaDescriptor(
        Descriptor.create("com.spotify", serviceName, version),
        apolloVersion);
  }

  private static Manifest getManifest(ClassLoader classLoader) throws IOException {
    try {
      Enumeration<URL> resources = classLoader.getResources("META-INF/MANIFEST.MF");

      while (resources.hasMoreElements()) {
        final URL url = resources.nextElement();
        final Manifest manifest = new Manifest(url.openStream());
        final Attributes mainAttributes = manifest.getMainAttributes();
        final String value = mainAttributes.getValue(APOLLO_VERSION);
        if (value != null) {
          return manifest;
        }
      }
    } catch (IOException e) {
      LOG.error("Failed to read manifest", e);
      throw new IOException("Failed to find manifest", e);
    }

    return null;
  }
}
