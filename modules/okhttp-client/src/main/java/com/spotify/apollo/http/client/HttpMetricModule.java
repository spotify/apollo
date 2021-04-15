/*-
 * -\-\-
 * Spotify Apollo okhttp Client Module
 * --
 * Copyright (C) 2021 Spotify AB
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

package com.spotify.apollo.http.client;

import com.google.inject.Inject;
import com.google.inject.multibindings.Multibinder;
import com.spotify.apollo.environment.ClientDecorator;
import com.spotify.apollo.environment.IncomingRequestAwareClient;
import com.spotify.apollo.module.AbstractApolloModule;
import com.spotify.apollo.module.ApolloModule;
import com.spotify.metrics.core.SemanticMetricRegistry;

public class HttpMetricModule extends AbstractApolloModule {

  public static ApolloModule create() {
    return new HttpMetricModule();
  }

  @Override
  public String getId() {
    return "http-metric-module";
  }

  @Override
  protected void configure() {
    Multibinder.newSetBinder(this.binder(), ClientDecorator.class)
        .addBinding()
        .to(HttpMetricClientDecorator.class);
  }

  public static class HttpMetricClientDecorator implements ClientDecorator {

    private final SemanticMetricRegistry metricsRegistry;

    @Inject
    HttpMetricClientDecorator(final SemanticMetricRegistry metricRegistry) {
      this.metricsRegistry = metricRegistry;
    }

    public IncomingRequestAwareClient apply(IncomingRequestAwareClient baseClient) {
      return new MetricsHttpClient(baseClient, this.metricsRegistry);
    }
  }
}
