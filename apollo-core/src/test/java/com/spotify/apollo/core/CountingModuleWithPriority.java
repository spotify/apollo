package com.spotify.apollo.core;

import com.spotify.apollo.module.AbstractApolloModule;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A simple module with configurable priority that increments a counter on config.
 *
 * Used for testing.
*/
class CountingModuleWithPriority extends AbstractApolloModule {

  private final double priority;
  private final AtomicInteger count;

  CountingModuleWithPriority(double priority, AtomicInteger count) {
    this.priority = priority;
    this.count = count;
  }

  @Override
  public double getPriority() {
    return priority;
  }

  @Override
  protected void configure() {
    count.incrementAndGet();
  }

  @Override
  public String getId() {
    return "prio-module";
  }
}
