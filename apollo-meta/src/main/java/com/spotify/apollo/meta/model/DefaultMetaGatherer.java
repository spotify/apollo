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


import com.typesafe.config.Config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.Nullable;

import static com.typesafe.config.ConfigFactory.empty;
import static com.typesafe.config.ConfigFactory.parseMap;
import static java.util.Collections.singletonMap;

public class DefaultMetaGatherer implements MetaGatherer {
  private static final Logger log = LoggerFactory.getLogger(DefaultMetaGatherer.class);

  public static final int DEFAULT_SIZE_LIMIT = 100;
  public static final String OVER_SIZE_KEY = "<over size limit>";

  private final int sizeLimit;
  private final long startTime = System.currentTimeMillis();

  private final Model.MetaInfo metaInfo;

  @Nullable
  private final Config config;

  private final CallsInfo endpoints;
  private final ConcurrentMap<String, CallsInfo> incoming = Maps.newConcurrentMap();
  private final ConcurrentMap<String, CallsInfo> outgoing = Maps.newConcurrentMap();

  DefaultMetaGatherer(Model.MetaInfo metaInfo) {
    this(DEFAULT_SIZE_LIMIT, metaInfo, null);
  }

  DefaultMetaGatherer(Model.MetaInfo metaInfo, Config config) {
    this(DEFAULT_SIZE_LIMIT, metaInfo, config);
  }

  private DefaultMetaGatherer(int sizeLimit, Model.MetaInfo metaInfo, @Nullable Config config) {
    this.sizeLimit = sizeLimit;
    this.endpoints = new CallsInfo(sizeLimit);

    this.config = config;
    this.metaInfo = MetaInfoBuilder.from(metaInfo)
        .systemVersion("java " + System.getProperty("java.version"))
        .build();
  }

  @Override
  public synchronized Model.MetaInfo info() {
    return MetaInfoBuilder.from(metaInfo)
        .serviceUptime((System.currentTimeMillis() - startTime) / 1000.0)
        .build();
  }

  @Override
  public synchronized Model.LoadedConfig loadedConfig() {
    // TODO: consider whether this should also display the 'apollo' configuration, somehow. It probably should.
    return new LoadedConfigBuilder()
        .spNode(filteredConfig().root())
        .build();
  }

  private Config filteredConfig() {
    if (config == null) {
      return empty();
    } else {
      if (!ConfigFilter.metaConfigEnabled(config)) {
        String disableMessage =
            "enable by adding '\"_meta\": { \"expose-config\": true },' to root level service "
            + "config. See https://github.com/spotify/apollo/tree/master/apollo-api-impl"
            + "/src/main/java/com/spotify/apollo/meta/model";
        return parseMap(singletonMap("disabled", disableMessage));
      } else {
        Set<String> filter = ConfigFilter.configFilter(config);
        return ConfigFilter.filterConfigObject(config.root(), filter)
            .toConfig();
      }
    }
  }

  @Override
  public synchronized Model.EndpointsInfo endpoints() {
    return endpoints.getEndpointsInfo();
  }

  @Override
  public synchronized Model.ExternalCallsInfo calls() {
    Model.ExternalCallsInfo calls = new Model.ExternalCallsInfo();
    for (Map.Entry<String, CallsInfo> entry : incoming.entrySet()) {
      calls.incoming.put(entry.getKey(), entry.getValue().getEndpointsInfo().endpoints);
    }
    for (Map.Entry<String, CallsInfo> entry : outgoing.entrySet()) {
      calls.outgoing.put(entry.getKey(), entry.getValue().getEndpointsInfo().endpoints);
    }
    return calls;
  }

  @Override
  public CallsGatherer getServiceCallsGatherer() {
    return endpoints;
  }

  @Override
  public CallsGatherer getIncomingCallsGatherer(String service) {
    if (service == null) {
      service = "<null>";
    }
    if (!incoming.containsKey(service)) {
      if (incoming.size() < sizeLimit) {
        incoming.putIfAbsent(service, new CallsInfo(sizeLimit));
      } else {
        if (!incoming.containsKey(OVER_SIZE_KEY)) {
          incoming.putIfAbsent(OVER_SIZE_KEY, new CallsInfo(sizeLimit));
        }
        return incoming.get(OVER_SIZE_KEY);
      }
    }
    return incoming.get(service);
  }

  @Override
  public CallsGatherer getOutgoingCallsGatherer(String service) {
    if (service == null) {
      service = "<null>";
    }
    if (!outgoing.containsKey(service)) {
      if (outgoing.size() < sizeLimit) {
        outgoing.putIfAbsent(service, new CallsInfo(sizeLimit));
      } else {
        if (!outgoing.containsKey(OVER_SIZE_KEY)) {
          outgoing.putIfAbsent(OVER_SIZE_KEY, new CallsInfo(sizeLimit));
        }
        return outgoing.get(OVER_SIZE_KEY);
      }
    }
    return outgoing.get(service);
  }
}
