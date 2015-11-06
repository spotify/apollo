/*
 * Copyright (c) 2013-2015 Spotify AB
 */

package com.spotify.apollo.request;

import com.spotify.apollo.environment.IncomingRequestAwareClient;
import com.spotify.apollo.dispatch.Endpoint;
import com.spotify.apollo.dispatch.EndpointInvocationHandler;
import com.spotify.apollo.meta.IncomingCallsGatherer;
import com.spotify.apollo.route.ApplicationRouter;

/**
 * Factory for an Apollo {@link RequestHandler}.
 */
public final class Handlers {

  private static final EndpointInvocationHandler ENDPOINT_INVOCATION_HANDLER =
      new EndpointInvocationHandler();

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
    return (request, requestContext, endpoint) ->
        () -> ENDPOINT_INVOCATION_HANDLER.handle(request, requestContext, endpoint);
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
