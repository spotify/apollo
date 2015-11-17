/*
 * -\-\-
 * Spotify Apollo API Interfaces
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
package com.spotify.apollo;

import java.io.Closeable;
import java.util.concurrent.CompletionStage;

import okio.ByteString;

/**
 * An Apollo service client.
 *
 * Clients are available from:
 * - {@link RequestContext#requestScopedClient()} within request handling, or
 * - {@link Environment#client()} otherwise.
 *
 */
public interface Client {

  /**
   * Send a Request and get an asynchronous Response as a CompletionStage.
   *
   * @param request  the request to send
   * @return a CompletionStage that completes normally with a {@link Response<ByteString>},
   *     or completes exceptionally if there is a failure sending the request.
   *     An error status code returned by the service is a normal completion.
   */
  CompletionStage<Response<ByteString>> send(Request request);
}
