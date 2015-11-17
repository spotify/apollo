package com.spotify.apollo.meta;

import org.junit.Test;

import static org.junit.Assert.assertTrue;
/*
 * Copyright (c) 2015 Spotify AB
 */

public class MetaDescriptorTest {

  @Test
  public void testLoadApolloVersion() throws Exception {
    ClassLoader classLoader = ClassLoader.getSystemClassLoader();
    String version = MetaDescriptor.loadApolloVersion(classLoader);
    assertTrue(version.startsWith("1.0.0"));
  }
}
