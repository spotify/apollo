/*
 * -\-\-
 * Spotify Apollo API Environment
 * --
 * Copyright (C) 2013 - 2016 Spotify AB
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

import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.Provides;

import com.spotify.apollo.module.ApolloModule;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.util.Set;

import static java.util.Collections.emptySet;

class ConfigModule implements ApolloModule {

  @Provides
  public Config config() {
    return ConfigFactory.empty();
  }

  @Override
  public String getId() {
    return "config-for-test";
  }

  @Override
  public double getPriority() {
    return 0;
  }

  @Override
  public Set<? extends Key<?>> getLifecycleManaged() {
    return emptySet();
  }

  @Override
  public void configure(Binder binder) {

  }
}
