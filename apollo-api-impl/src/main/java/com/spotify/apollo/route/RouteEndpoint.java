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
