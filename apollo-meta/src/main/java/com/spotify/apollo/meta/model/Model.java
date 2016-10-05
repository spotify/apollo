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

import com.google.common.collect.Maps;

import com.typesafe.config.ConfigObject;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import io.norberg.automatter.AutoMatter;

/**
 * Data model for application metadata.
 */
public interface Model {

  @AutoMatter
  interface MetaInfo {

    /**
     * A unique-ish identifier of the running software.
     */
    String componentId();
    String buildVersion();
    String containerVersion();
    @Nullable String systemVersion();

    double serviceUptime();
  }

  @AutoMatter
  interface LoadedConfig {
    ConfigObject spNode();
  }

  class EndpointsInfo {
    public String docstring;
    public List<EndpointInfo> endpoints;
  }

  class EndpointInfo {
    public String methodName;
    public String uri;
    public List<String> method;
    public String docstring;
    public List<QueryParameter> queryParameters;
  }

  class ExternalCallsInfo {
    public Map<String, List<EndpointInfo>> incoming = Maps.newTreeMap();
    public Map<String, List<EndpointInfo>> outgoing = Maps.newTreeMap();
  }

  class QueryParameter {
    public final String name;

    public QueryParameter(String name) {
      this.name = name;
    }
  }
}
