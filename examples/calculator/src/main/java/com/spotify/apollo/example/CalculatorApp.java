package com.spotify.apollo.example;

import com.spotify.apollo.Environment;
import com.spotify.apollo.Request;
import com.spotify.apollo.Response;
import com.spotify.apollo.Status;
import com.spotify.apollo.httpservice.HttpService;
import com.spotify.apollo.httpservice.LoadingException;
import com.spotify.apollo.route.AsyncHandler;
import com.spotify.apollo.route.Middleware;
import com.spotify.apollo.route.Route;
import com.spotify.apollo.route.SyncHandler;

import java.util.Optional;

/**
 * This example demonstrates a simple calculator service.
 *
 * It uses a synchronous route to evaluate addition and a middleware
 * that translates uncaught exceptions into error code 418.
 *
 * Try it by calling it with query parameters that are not parsable integers.
 */
final class CalculatorApp {

  public static void main(String... args) throws LoadingException {
    HttpService.boot(CalculatorApp::init, "calculator-service", args);
  }

  static void init(Environment environment) {
    SyncHandler<Response<Integer>> addHandler = context -> add(context.request());

    environment.routingEngine()
        .registerAutoRoute(Route.with(exceptionHandler(), "GET", "/add", addHandler))
        .registerAutoRoute(Route.sync("GET", "/unsafeadd", addHandler));
  }

  /**
   * A simple adder of request parameters {@code t1} and {@code t2}
   *
   * @param request  The request to handle the addition for
   * @return A response of an integer representing the sum
   */
  static Response<Integer> add(Request request) {
    Optional<String> t1 = request.parameter("t1");
    Optional<String> t2 = request.parameter("t2");
    if (t1.isPresent() && t2.isPresent()) {
      int result = Integer.valueOf(t1.get()) + Integer.valueOf(t2.get());
      return Response.forPayload(result);
    } else {
      return Response.forStatus(Status.BAD_REQUEST);
    }
  }

  /**
   * A generic middleware that maps uncaught exceptions to error code 418
   */
  static <T> Middleware<SyncHandler<Response<T>>, SyncHandler<Response<T>>> exceptionMiddleware() {
    return handler -> requestContext -> {
      try {
        return handler.invoke(requestContext);
      } catch (RuntimeException e) {
        return Response.forStatus(Status.IM_A_TEAPOT);
      }
    };
  }

  /**
   * Async version of {@link #exceptionMiddleware()}
   */
  static <T> Middleware<SyncHandler<Response<T>>, AsyncHandler<Response<T>>> exceptionHandler() {
    return CalculatorApp.<T>exceptionMiddleware().and(Middleware::syncToAsync);
  }
}
