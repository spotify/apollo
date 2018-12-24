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

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import com.spotify.apollo.core.Services;
import com.spotify.apollo.meta.MetaDescriptor;
import com.spotify.apollo.meta.MetaInfoTracker;
import com.typesafe.config.Config;

import java.io.IOException;

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
