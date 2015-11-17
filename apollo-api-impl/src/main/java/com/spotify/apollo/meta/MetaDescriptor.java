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

  public static MetaDescriptor readMetaDescriptor(String serviceName, ClassLoader classLoader)
      throws IOException {
    String apolloVersion;
    String version;

    try {
      apolloVersion = loadApolloVersion(classLoader);
    } catch (IOException e) {
      apolloVersion = "0.0.0-UNKNOWN";
    }

    version = loadVersion(classLoader);
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

  /**
   * Tries to load the first "Implementation-Version" manifest entry it can find in the given
   * classloader.
   *
   * @param classLoader  The classloader to load manifests from
   * @return a version string
   * @throws IOException
   */
  private static String loadVersion(ClassLoader classLoader) throws IOException {
    try {
      Enumeration<URL> resources = classLoader.getResources("META-INF/MANIFEST.MF");

      while (resources.hasMoreElements()) {
        final URL url = resources.nextElement();
        final Manifest manifest = new Manifest(url.openStream());
        final Attributes mainAttributes = manifest.getMainAttributes();
        final String value = mainAttributes.getValue(IMPL_VERSION);

        if (value != null) {
          return value;
        }
      }

    } catch (IOException e) {
      LOG.error("Failed to read manifest", e);
      throw new IOException("Failed to find manifest", e);
    }

    return null;
  }
}
