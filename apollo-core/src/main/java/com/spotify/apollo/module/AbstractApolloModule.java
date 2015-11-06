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
