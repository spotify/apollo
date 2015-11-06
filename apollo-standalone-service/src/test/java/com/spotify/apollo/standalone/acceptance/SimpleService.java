/*
 * Copyright (c) 2014 Spotify AB
 */

package com.spotify.apollo.standalone.acceptance;

import com.spotify.apollo.AppInit;
import com.spotify.apollo.Client;
import com.spotify.apollo.Environment;
import com.spotify.apollo.Request;
import com.spotify.apollo.RequestContext;
import com.spotify.apollo.route.Route;
import com.spotify.apollo.standalone.LoadingException;
import com.spotify.apollo.standalone.StandaloneService;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

class SimpleService implements AppInit, ServiceStepdefs.BootedApplication {

  private String pod;

  @Override
  public Optional<String> pod() {
    return Optional.ofNullable(pod);
  }

  public static void main(String[] args) throws LoadingException {
    StandaloneService.boot(new SimpleService(), "ping", "run", "pod", "-vv");
  }

  @Override
  public void create(Environment environment) {
    pod = environment.domain();

    environment.routingEngine()
        .registerRoute(
            Route.sync("GET", "/greet/<arg>",
                       context -> handle(context.request(), context.pathArgs().get("arg")))
                .withDocString(
                    "Responds with pod and argument.",
                    "Pod, (domain from the app environment) and" +
                    " the call argument are baked into a free-form string."))
        .registerRoute(
            Route.sync("GET", "/uriencodingtest/<parameter>",
                        context -> testUri(context.pathArgs().get("parameter"))));
  }

  public String handle(Request request, String arg) {
    return "pod: " + pod + ", pong: " + arg;
  }

  public String testUri(final String parameter) {
    return parameter;
  }
}
