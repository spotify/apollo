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
package com.spotify.apollo.metrics;

import com.spotify.apollo.Client;
import com.spotify.apollo.RequestContext;
import com.spotify.apollo.environment.EndpointRunnableFactoryDecorator;
import com.spotify.apollo.request.EndpointRunnableFactory;
import com.spotify.apollo.request.RequestContexts;
import com.spotify.apollo.request.TrackedOngoingRequest;

import javax.inject.Inject;

/**
 * An {@link EndpointRunnableFactory} that collects metrics
 */
class MetricsCollectingEndpointRunnableFactoryDecorator implements EndpointRunnableFactoryDecorator {

  private final ServiceMetrics metrics;

  @Inject
  MetricsCollectingEndpointRunnableFactoryDecorator(ServiceMetrics metrics) {
    this.metrics = metrics;
  }

  @Override
  public EndpointRunnableFactory apply(EndpointRunnableFactory delegate) {
    return (request, requestContext, endpoint) -> {
      final String endpointName = endpoint.info().getName();

      // note: will not time duration of matching and dispatching
      final RequestMetrics requestStats = metrics.metricsForEndpointCall(endpointName);

      requestStats.incoming(request.request());

      final TrackedOngoingRequest
          trackedRequest = new MetricsTrackingOngoingRequest(requestStats, request);
      final Client instrumentingClient =
          new InstrumentingClient(requestContext.requestScopedClient(), trackedRequest);
      final RequestContext instrumentingContext =
          RequestContexts.create(
              requestContext.request(),
              instrumentingClient,
              requestContext.pathArgs());

      return delegate.create(trackedRequest, instrumentingContext, endpoint);
    };
  }
}
