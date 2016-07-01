/*
 * -\-\-
 * Spotify Apollo Metrics Module
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
package com.spotify.apollo.metrics.noop;

import com.spotify.apollo.metrics.ServiceMetrics;
import com.spotify.apollo.metrics.MetricsFactory;

public class NoopMetricsFactory implements MetricsFactory {

  private NoopMetricsFactory() {
  }

  private static final NoopMetricsFactory INSTANCE = new NoopMetricsFactory();

  public static MetricsFactory instance() {
    return INSTANCE;
  }

  @Override
  public ServiceMetrics createForService(String serviceName) {
      return NoopServiceMetrics.instance();
  }
}
