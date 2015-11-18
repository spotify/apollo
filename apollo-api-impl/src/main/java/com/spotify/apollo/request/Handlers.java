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
package com.spotify.apollo.request;

import com.spotify.apollo.dispatch.Endpoint;
import com.spotify.apollo.dispatch.EndpointInvocationHandler;
import com.spotify.apollo.environment.IncomingRequestAwareClient;
import com.spotify.apollo.meta.IncomingCallsGatherer;
import com.spotify.apollo.route.ApplicationRouter;

/**
 * Factory for an Apollo {@link RequestHandler}.
 */
public final class Handlers {

  private Handlers() {
  }

  public static RequestHandler requestHandler(
      RequestRunnableFactory requestRunnableFactory,
      EndpointRunnableFactory endpointRunnableFactory,
      IncomingRequestAwareClient client) {

    return new RequestHandlerImpl(
        requestRunnableFactory,
        endpointRunnableFactory,
        client);
  }

  public static RequestRunnableFactory requestRunnableFactory(
      ApplicationRouter<Endpoint> applicationRouter) {

    return request -> new RequestRunnableImpl(request, applicationRouter);
  }

  public static EndpointRunnableFactory endpointRunnableFactory() {
    return new EndpointInvocationHandler();
  }

  public static EndpointRunnableFactory withTracking(
      EndpointRunnableFactory endpointRunnableFactory,
      IncomingCallsGatherer incomingCallsGatherer,
      RequestTracker requestTracker) {

    return new TrackingEndpointRunnableFactory(
        endpointRunnableFactory,
        incomingCallsGatherer,
        requestTracker);
  }
}
