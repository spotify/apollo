/*
 * -\-\-
 * Spotify Apollo Service Core (aka Leto)
 * --
 * Copyright (C) 2013 - 2021 Spotify AB
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
package com.spotify.apollo.core.spring;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.springframework.core.env.EnumerablePropertySource;

public class ApolloPropertySource extends EnumerablePropertySource<Config> {

  private final Config config;

  public ApolloPropertySource(String configName) {
    super("lightbendSpotifyConfig");
    this.config = ConfigFactory.load(configName);
  }

  @Override
  public String[] getPropertyNames() {
    return config.root().keySet().toArray(new String[0]);
  }

  @Override
  public Object getProperty(String name) {
    if (name.contains("[") || name.contains(":")) {
      return null;
    }
    return config.hasPath(name)? config.getAnyRef(name) : null;
  }
}
