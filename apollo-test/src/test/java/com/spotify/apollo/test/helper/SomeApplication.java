/*
 * -\-\-
 * Spotify Apollo Testing Helpers
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
package com.spotify.apollo.test.helper;

import com.spotify.apollo.Client;
import com.spotify.apollo.Environment;
import com.spotify.apollo.Request;
import com.spotify.apollo.route.AsyncHandler;
import com.spotify.apollo.route.Route;
import com.spotify.apollo.route.RouteProvider;

import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;

import static com.spotify.apollo.route.Route.async;
import static com.spotify.apollo.route.Route.sync;

final class SomeApplication implements RouteProvider {
  private final String domain;
  private final String someValue;
  private final SomeService someService;

  private SomeApplication(Environment environment, SomeService someService, CloseCall closeCall) {
    this.someValue = "no value found for some.key";
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
