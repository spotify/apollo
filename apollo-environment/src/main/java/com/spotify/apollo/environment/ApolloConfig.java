/*
 * -\-\-
 * Spotify Apollo API Environment
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
package com.spotify.apollo.environment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Configuration object for common Apollo configurable settings.
 */
public class ApolloConfig {

  private static final Logger LOG = LoggerFactory.getLogger(ApolloConfig.class);

  private final String backend;
  private final boolean enableIncomingRequestLogging;
  private final boolean enableOutgoingRequestLogging;
  private final boolean enableMetaApi;

  public ApolloConfig(String backend, boolean enableIncomingRequestLogging,
                      boolean enableOutgoingRequestLogging, boolean enableMetaApi) {
    this.backend = backend;
    this.enableIncomingRequestLogging = enableIncomingRequestLogging;
    this.enableOutgoingRequestLogging = enableOutgoingRequestLogging;
    this.enableMetaApi = enableMetaApi;
  }

  public String backend() {
    return backend;
  }

  public boolean enableIncomingRequestLogging() {
    return enableIncomingRequestLogging;
  }

  public boolean enableOutgoingRequestLogging() {
    return enableOutgoingRequestLogging;
  }

  public boolean enableMetaApi() {
    return enableMetaApi;
  }

  public static ApolloConfig forDomain(String backend) {
    return new ApolloConfig(backend, true, true, true);
  }

  public ApolloConfig overriddenBy(Map<String, String> overrides) {
    String newBackend = backend;
    Boolean newEnableIncomingRequestLogging = enableIncomingRequestLogging;
    Boolean newEnableOutgoingRequestLogging = enableOutgoingRequestLogging;
    Boolean newEnableMetaApi= enableMetaApi;

    for (Map.Entry<String, String> entry : overrides.entrySet()) {
      switch (entry.getKey()) {
        case "backend":
          newBackend = entry.getValue();
          break;
        case "enableIncomingRequestLogging":
          newEnableIncomingRequestLogging = Boolean.valueOf(entry.getValue());
          break;
        case "enableOutgoingRequestLogging":
          newEnableOutgoingRequestLogging = Boolean.valueOf(entry.getValue());
          break;
        case "enableMetaApi":
          newEnableMetaApi = Boolean.valueOf(entry.getValue());
          break;
        default:
          LOG.warn("unrecognised apollo config key: '{}' (value {})", entry.getKey(),
                   entry.getValue());

      }
    }

    return new ApolloConfig(newBackend, newEnableIncomingRequestLogging, newEnableOutgoingRequestLogging, newEnableMetaApi);
  }
}
