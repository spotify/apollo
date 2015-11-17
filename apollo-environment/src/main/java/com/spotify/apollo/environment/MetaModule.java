/*
 * Copyright (c) 2013-2015 Spotify AB
 */

package com.spotify.apollo.environment;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.name.Named;

import com.spotify.apollo.core.Services;
import com.spotify.apollo.meta.MetaDescriptor;
import com.spotify.apollo.meta.MetaInfoTracker;
import com.typesafe.config.Config;

import java.io.IOException;

import javax.inject.Singleton;

/**
 * Module for setting up service metadata collection objects.
 */
class MetaModule extends AbstractModule {

  @Override
  protected void configure() {
  }

  @Provides
  @Singleton
  public MetaDescriptor metaDescriptor(@Named(Services.INJECT_SERVICE_NAME) String serviceName)
      throws IOException {

    final ClassLoader classLoader = getClass().getClassLoader();
    return MetaDescriptor.readMetaDescriptor(serviceName, classLoader);
  }

  @Provides
  @Singleton
  private MetaInfoTracker metaInfoTracker(Config configNode, MetaDescriptor metaDescriptor) {
    return new MetaInfoTracker(
        metaDescriptor.descriptor(),
        "apollo-standalone " + metaDescriptor.apolloVersion(), // FIXME: no 'standalone' here
        configNode);
  }
}
