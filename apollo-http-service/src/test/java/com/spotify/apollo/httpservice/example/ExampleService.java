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
package com.spotify.apollo.httpservice.example;

import com.spotify.apollo.Environment;
import com.spotify.apollo.core.Service;
import com.spotify.apollo.route.Route;
import com.spotify.apollo.httpservice.LoadingException;
import com.spotify.apollo.httpservice.HttpService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ExampleService {

  private static final Logger LOG = LoggerFactory.getLogger(ExampleService.class);

  public static void main(String[] args) throws LoadingException {

    final Service service = HttpService.usingAppInit(ExampleService::init, "ping")
        .build();

    HttpService.boot(service, args);
    LOG.info("bye bye");
  }

  public static void init(Environment environment) {
    environment.routingEngine()
        .registerAutoRoute(Route.sync("GET", "/hello", c -> "hello world"));
    LOG.info("in app init {}", environment);
  }
}
