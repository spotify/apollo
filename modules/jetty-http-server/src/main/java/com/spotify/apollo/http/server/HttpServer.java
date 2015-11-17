/*
 * -\-\-
 * Spotify Apollo Jetty HTTP Server Module
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
package com.spotify.apollo.http.server;

import com.spotify.apollo.request.RequestHandler;

import java.io.Closeable;

/**
 * A fully configured server that can be started and stopped.
 */
public interface HttpServer extends Closeable {

  /**
   * Start the server using the given {@link RequestHandler}.
   *
   * @param requestHandler  The request handler that should handle server requests
   */
  void start(RequestHandler requestHandler);
}
