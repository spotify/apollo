/*
 * -\-\-
 * Spotify Apollo Service Core (aka Leto)
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
package com.spotify.apollo.module;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.Key;

import java.util.Set;

/**
 * A base implementation of an Apollo module.
 */
public abstract class AbstractApolloModule extends AbstractModule implements ApolloModule {

  private final ImmutableSet.Builder<Key<?>> lifecycleManagedBuilder;

  protected AbstractApolloModule() {
    lifecycleManagedBuilder = ImmutableSet.builder();
  }

  protected void manageLifecycle(Key<?> key) {
    lifecycleManagedBuilder.add(key);
  }

  protected void manageLifecycle(Class<?> cls) {
    lifecycleManagedBuilder.add(Key.get(cls));
  }

  @Override
  protected void install(final Module module) {
    super.install(module);
    if (module instanceof ApolloModule) {
      for (Key<?> key : ((ApolloModule) module).getLifecycleManaged()) {
        manageLifecycle(key);
      }
    }
  }

  @Override
  public double getPriority() {
    return 0.0;
  }

  @Override
  public Set<? extends Key<?>> getLifecycleManaged() {
    return lifecycleManagedBuilder.build();
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("id", getId())
        .toString();
  }
}
