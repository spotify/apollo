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
package com.spotify.apollo.httpservice;

import com.spotify.apollo.Environment;
import com.spotify.apollo.route.Route;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class MinimalRunner {

  private static final Logger LOG = LoggerFactory.getLogger(MinimalRunner.class);

  /**
   * Typical entry point of a service
   *
   * @param args  Program arguments
   */
  public static void main(String[] args) throws Exception {
    HttpService.boot(MinimalRunner::app, "test", "run", "foo", "-Dhttp.server.port=8080");
  }

  public static void app(Environment env) {
    env.routingEngine()
        .registerRoute(Route.sync("GET", "/", (requestContext) -> "Hello World"));

    env.closer()
        .register(() -> LOG.info("Goodbye World"));
  }
}
