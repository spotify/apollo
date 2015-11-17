/*
 * -\-\-
 * Spotify Apollo Service Core (aka Leto)
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
package com.spotify.apollo.example;

import com.spotify.apollo.Environment;
import com.spotify.apollo.RequestContext;
import com.spotify.apollo.Response;
import com.spotify.apollo.Status;
import com.spotify.apollo.httpservice.HttpService;
import com.spotify.apollo.httpservice.LoadingException;
import com.spotify.apollo.route.Middleware;
import com.spotify.apollo.route.Route;
import com.spotify.apollo.route.SyncHandler;

import java.util.Optional;

/**
 * This example demonstrates a simple calculator service.
 *
 * It uses a synchronous route to evaluate addition and a middleware
 * that translates uncaught exceptions into error code 418.
 */
final class CalculatorApp {

  public static void main(String[] args) throws LoadingException {
    HttpService.boot(CalculatorApp::init, "calculator-service", args);
  }

  static void init(Environment environment) {
    environment.routingEngine()
        .registerRoute(Route.with(
            exceptionMiddleware().and(Middleware::syncToAsync), "GET", "/add", CalculatorApp::add))
        .registerRoute(Route.sync("GET", "/unsafeadd", CalculatorApp::add));
  }

  /**
   * A generic middleware that maps uncaught exceptions to error code 418
   */
  static Middleware<SyncHandler<Response<Integer>>, SyncHandler<Response<Integer>>> exceptionMiddleware() {
    return handler -> requestContext -> {
      try {
        return handler.invoke(requestContext);
      } catch (RuntimeException e) {
        return Response.forStatus(Status.IM_A_TEAPOT);
      }
    };
  }

  static Response<Integer> add(RequestContext context) {
    Optional<String> t1 = context.request().parameter("t1");
    Optional<String> t2 = context.request().parameter("t2");
    if (t1.isPresent() && t2.isPresent()) {
      int result = Integer.valueOf(t1.get()) + Integer.valueOf(t2.get());
      return Response.forPayload(result);
    } else {
      return Response.forStatus(Status.BAD_REQUEST);
    }
  }
}
