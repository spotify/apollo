/*
 * Copyright (c) 2015 Spotify AB
 */

package com.spotify.apollo.test.helper;

import com.spotify.apollo.Environment;
import com.spotify.apollo.Request;
import com.spotify.apollo.Client;
import com.spotify.apollo.environment.ConfigUtil;
import com.spotify.apollo.route.AsyncHandler;
import com.spotify.apollo.route.Route;
import com.spotify.apollo.route.RouteProvider;

import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;

import static com.spotify.apollo.environment.ConfigUtil.*;
import static com.spotify.apollo.route.Route.async;
import static com.spotify.apollo.route.Route.sync;

final class SomeApplication implements RouteProvider {
  private final String domain;
  private final String someValue;
  private final SomeService someService;

  private SomeApplication(Environment environment, SomeService someService, CloseCall closeCall) {
    this.someValue = optionalString(environment.config(), "some.key")
        .orElse("no value found for some.key");
    this.someService = someService;
    this.domain = environment.domain();

    environment.closer().register(closeCall::didClose);
  }

  static SomeApplication create(
      Environment environment,
      SomeService someService,
      CloseCall closeCall) {
    return new SomeApplication(environment, someService, closeCall);
  }

  @Override
  public Stream<Route<? extends AsyncHandler<?>>> routes() {
    return Stream.of(
        sync("GET", "/", requestContext -> someService.thatDoesThings()),
        sync("GET", "/conf-key", requestContext -> someValue),
        sync("GET", "/domain", requestContext -> domain),
        async("GET", "/call/<url:path>", requestContext ->
            call(requestContext.requestScopedClient(),
                 "http://" + requestContext.pathArgs().get("url"))),
        sync("GET", "/uri", requestContext -> requestContext.request().uri()),
        sync("POST", "/post", requestContext ->
            requestContext.request().payload().get()));
  }

  private static CompletionStage<String> call(Client client, String url) {
    return client.send(Request.forUri(url))
        .thenApply(reply -> reply.payload().get().utf8());
  }

  interface SomeService {

    String thatDoesThings();
  }

  interface CloseCall {

    void didClose();
  }
}
