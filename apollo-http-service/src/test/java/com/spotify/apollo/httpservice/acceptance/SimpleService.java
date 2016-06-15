/*
 * -\-\-
 * Spotify Apollo HTTP Service
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
package com.spotify.apollo.httpservice.acceptance;

import com.spotify.apollo.AppInit;
import com.spotify.apollo.Client;
import com.spotify.apollo.Environment;
import com.spotify.apollo.Request;
import com.spotify.apollo.httpservice.HttpServiceConfiguration;
import com.spotify.apollo.route.Route;
import com.spotify.apollo.httpservice.LoadingException;
import com.spotify.apollo.httpservice.HttpService;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

import okio.ByteString;

class SimpleService implements AppInit, ServiceStepdefs.BootedApplication {

  private String pod;
  private String reverserAddress;

  @Override
  public Optional<String> pod() {
    return Optional.ofNullable(pod);
  }

  public static void main(String... args) throws LoadingException {
    HttpService.boot(new SimpleService(), "ping", HttpServiceConfiguration.create("pod"), "run", "pod", "-vv");
  }

  @Override
  public void create(Environment environment) {
    pod = environment.domain();

    reverserAddress = environment.config().getString("reverser.address");

    environment.routingEngine()
        .registerAutoRoute(
            Route.sync("GET", "/greet/<arg>",
                       context -> handle(context.request(), context.pathArgs().get("arg")))
                .withDocString(
                    "Responds with pod and argument.",
                    "Pod, (domain from the app environment) and" +
                    " the call argument are baked into a free-form string."))
        .registerAutoRoute(
            Route.sync("GET", "/reverse/<arg>",
                       context -> reverse(context.requestScopedClient(), context.pathArgs().get("arg"))))
        .registerAutoRoute(
            Route.sync("GET", "/uriencodingtest/<parameter>",
                        context -> testUri(context.pathArgs().get("parameter"))));
  }

  private String reverse(Client client, String input) {
    try {
      return client.send(Request.forUri(reverserAddress + "/" + input))
          .toCompletableFuture().get()
          .payload().map(ByteString::utf8)
          .orElse("nooo, where's my payload??");
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  public String handle(Request request, String arg) {
    return "pod: " + pod + ", pong: " + arg;
  }

  public String testUri(final String parameter) {
    return parameter;
  }
}
