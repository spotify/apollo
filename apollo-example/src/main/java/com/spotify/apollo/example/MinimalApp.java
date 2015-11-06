/*
 * Copyright (c) 2013-2015 Spotify AB
 */

package com.spotify.apollo.example;

import com.spotify.apollo.Environment;
import com.spotify.apollo.Request;
import com.spotify.apollo.RequestContext;
import com.spotify.apollo.Response;
import com.spotify.apollo.Status;
import com.spotify.apollo.route.AsyncHandler;
import com.spotify.apollo.route.Middleware;
import com.spotify.apollo.route.Route;

import java.util.concurrent.CompletionStage;

import okio.ByteString;

import static java.util.concurrent.CompletableFuture.completedFuture;

final class MinimalApp {

  public static void main(String[] args) {
    // TODO: fix this when we have an HTTP-based 'standalone' app
//    StandaloneService.boot(MinimalApp::init, "test", args);
  }

  static void init(Environment environment) {
    environment.routingEngine()
        .registerRoute(Route.async("GET", "/beer", MinimalApp::beer))
        .registerRoute(Route.async("POST", "/beer", MinimalApp::beer)) // don't care about payload
        .registerRoute(Route.async("GET", "/flaky-beer", MinimalApp::beer)
                       .withMiddleware(flaky(.5f)));
  }

  static Middleware<AsyncHandler<Response<String>>, AsyncHandler<Response<String>>> flaky(float chance) {
    return handler -> requestContext -> {
      if (Math.random() <= chance) {
        return completedFuture(Response.forStatus(Status.SERVICE_UNAVAILABLE));
      }

      return handler.invoke(requestContext);
    };
  }

  static CompletionStage<Response<String>> beer(RequestContext context) {
    Request breweryRequest = Request.forUri("http://brewery/order");
    CompletionStage<Response<ByteString>> orderRequest = context.requestScopedClient()
        .send(breweryRequest);

    return orderRequest.thenApply(
        orderResponse -> {
          if (orderResponse.statusCode() != Status.OK) {
            return Response.forStatus(Status.INTERNAL_SERVER_ERROR);
          }

          // assume we get an order id as a plaintext payload
          final String orderId = orderResponse.payload().get().utf8();

          return Response.forPayload("your order is " + orderId);
        });
  }
}
