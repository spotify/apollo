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
import com.google.inject.Key;

import java.util.Set;

/**
 * A base implementation of an Apollo module.
 */
public abstract class AbstractApolloModule extends AbstractModule implements ApolloModule {

  private final ImmutableSet.Builder<Key<?>> preLifecycleManagedBuilder;
  private final ImmutableSet.Builder<Key<?>> postLifecycleManagedBuilder;

  protected AbstractApolloModule() {
    preLifecycleManagedBuilder = ImmutableSet.builder();
    postLifecycleManagedBuilder = ImmutableSet.builder();
  }

  protected void manageLifecycle(Key<?> key) {
    postLifecycleManagedBuilder.add(key);
  }

  protected void manageLifecycle(Class<?> cls) {
    postLifecycleManagedBuilder.add(Key.get(cls));
  }

  protected void manageLifecyclePre(Key<?> key) {
    preLifecycleManagedBuilder.add(key);
  }

  protected void manageLifecyclePre(Class<?> cls) {
    preLifecycleManagedBuilder.add(Key.get(cls));
  }

  @Override
  public double getPriority() {
    return 0.0;
  }

  @Override
  public Set<? extends Key<?>> getLifecycleManaged() {
    return postLifecycleManagedBuilder.build();
  }

  @Override
  public Set<? extends Key<?>> getLifecycleManagedPre() {
    return preLifecycleManagedBuilder.build();
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("id", getId())
        .toString();
  }
}
