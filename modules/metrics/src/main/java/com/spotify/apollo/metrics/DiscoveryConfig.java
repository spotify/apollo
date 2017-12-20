/*
 * -\-\-
 * Spotify Apollo Metrics
 * --
 * Copyright (C) 2013 - 2017 Spotify AB
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
package com.spotify.apollo.metrics;

import com.spotify.ffwd.http.HttpDiscovery;
import com.typesafe.config.Config;
import java.util.Collections;

public interface DiscoveryConfig {
  /**
   * Build an HTTP discovery component.
   */
  HttpDiscovery toHttpDiscovery();

  static DiscoveryConfig fromConfig(final Config config) {
    final String type = config.getString("type");

    switch (type) {
      case "static":
        final String host = config.getString("host");
        final int port = config.getInt("port");
        return new Static(host, port);
      case "srv":
        final String record = config.getString("record");
        return new Srv(record);
      default:
        throw new RuntimeException("Unrecognized discovery type: " + type);
    }
  }

  class Static implements DiscoveryConfig {
    private final String host;
    private final int port;

    Static(final String host, final int port) {
      this.host = host;
      this.port = port;
    }

    String getHost() {
      return host;
    }

    int getPort() {
      return port;
    }

    @Override
    public HttpDiscovery toHttpDiscovery() {
      final HttpDiscovery.HostAndPort server = new HttpDiscovery.HostAndPort(host, port);
      return new HttpDiscovery.Static(Collections.singletonList(server));
    }
  }

  class Srv implements DiscoveryConfig {
    private final String record;

    Srv(final String record) {
      this.record = record;
    }

    String getRecord() {
      return record;
    }

    @Override
    public HttpDiscovery toHttpDiscovery() {
      return new HttpDiscovery.Srv(record);
    }
  }
}
