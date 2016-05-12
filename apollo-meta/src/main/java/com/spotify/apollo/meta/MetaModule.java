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
package com.spotify.apollo.meta;

import com.google.inject.Provides;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;

import com.spotify.apollo.core.Services;
import com.spotify.apollo.environment.ClientDecorator;
import com.spotify.apollo.environment.ClientDecorator.Id;
import com.spotify.apollo.module.AbstractApolloModule;
import com.typesafe.config.Config;

import java.io.IOException;

import javax.inject.Singleton;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Module for setting up service metadata collection objects.
 */
public class MetaModule extends AbstractApolloModule {

  public static final Id OUTGOING_CALLS = Id.of(MetaModule.class, "Outgoing calls decorator");

  private final String assemblyName;

  private MetaModule(String assemblyName) {
    this.assemblyName = checkNotNull(assemblyName);
  }

  @Override
  protected void configure() {
    Multibinder.newSetBinder(binder(), ClientDecorator.class)
        .addBinding().to(OutgoingCallsDecorator.class);
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
    return new MetaInfoTrackerImpl(
        metaDescriptor.descriptor(),
        assemblyName + metaDescriptor.apolloVersion(),
        configNode);
  }

  @Provides
  @Singleton
  private OutgoingCallsDecorator outgoingCallsDecorator(MetaInfoTracker metaInfoTracker) {
    return new OutgoingCallsDecorator(metaInfoTracker.outgoingCallsGatherer());
  }

  @Override
  public String getId() {
    return "meta";
  }

  public static MetaModule create(String assemblyName) {
    return new MetaModule(assemblyName);
  }
}
