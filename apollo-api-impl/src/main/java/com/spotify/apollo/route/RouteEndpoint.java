/**
 * Copyright (C) 2013 Spotify AB
 */

package com.spotify.apollo.route;

import com.spotify.apollo.RequestContext;
import com.spotify.apollo.Response;
import com.spotify.apollo.dispatch.Endpoint;
import com.spotify.apollo.dispatch.EndpointInfo;

import java.util.concurrent.CompletionStage;

import okio.ByteString;

/**
 * Method endpoint identified by specific URI and URI parameters
 * for handling matching requests.
 */
class RouteEndpoint implements Endpoint {

  private final Route<? extends AsyncHandler<Response<ByteString>>> target;
  private final String methodName;

  RouteEndpoint(Route<? extends AsyncHandler<Response<ByteString>>> target) {
    this(target, "invoke");
  }

  RouteEndpoint(Route<? extends AsyncHandler<Response<ByteString>>> target, String methodName) {
    this.target = target;
    this.methodName = methodName;
  }

  @Override
  public EndpointInfo info() {
    return new EndpointInfo(
        target.uri(),
        target.method(),
        methodName,
        target.docString());
  }

  @Override
  public CompletionStage<Response<ByteString>> invoke(RequestContext requestContext) {
    return target.handler()
        .invoke(requestContext);
  }
}
