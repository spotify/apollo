/*
 * -\-\-
 * Spotify Apollo API Implementations
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
package com.spotify.apollo.meta.model;

import com.google.common.collect.Sets;


import com.typesafe.config.Config;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueType;

import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.typesafe.config.ConfigValueFactory.fromAnyRef;

/**
 * Implementation of config filtering.
 */
final class ConfigFilter {

  private ConfigFilter() {
    // no instantiation
  }

  static boolean metaConfigEnabled(Config rootConfig) {
    checkNotNull(rootConfig);

    return rootConfig.hasPath("_meta.expose-config") &&
           rootConfig.getBoolean("_meta.expose-config");
  }

  static Set<String> configFilter(Config rootConfig) {
    checkNotNull(rootConfig);

    Set<String> filter = Sets.newLinkedHashSet();
    filter.add("passw");
    filter.add("secret");
    filter.add("private");

    if (rootConfig.hasPath("_meta.config-filter")) {
      final Config configFilter = rootConfig.getConfig("_meta.config-filter");
      for (Map.Entry<String, ConfigValue> filterEntry : configFilter.entrySet()) {
        if (filterEntry.getValue().unwrapped() == Boolean.TRUE) {
          filter.add(filterEntry.getKey());
        }
      }
    }

    return filter;
  }

  static ConfigObject filterConfigObject(ConfigObject config, Set<String> filter) {
    ConfigObject result = config;

    for (Map.Entry<String, ConfigValue> entry : config.entrySet()) {
      String key = entry.getKey();
      final ConfigValue filteredValue;

      if (!isFiltered(key, filter)) {
        final ConfigValue value = entry.getValue();

        if (!"_meta".equals(key) && value.valueType() == ConfigValueType.OBJECT) {
          filteredValue = filterConfigObject((ConfigObject) value, filter);
        } else {
          filteredValue = value;
        }
      } else {
        filteredValue = fromAnyRef("*******", "filtered by config-filter settings");
      }

      result = result.withValue(key, filteredValue);
    }

    return result;
  }

  private static boolean isFiltered(String key, Set<String> filter) {
    for (String filterSubstring : filter) {
      if (key.contains(filterSubstring)) {
        return true;
      }
    }
    return false;
  }
}
