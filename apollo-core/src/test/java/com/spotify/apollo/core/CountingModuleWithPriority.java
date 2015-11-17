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
