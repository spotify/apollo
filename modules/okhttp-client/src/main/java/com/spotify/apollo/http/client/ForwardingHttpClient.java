/*
 * -\-\-
 * Spotify Apollo okhttp Client Module
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
package com.spotify.apollo.http.client;

import com.spotify.apollo.Request;
import com.spotify.apollo.Response;
import com.spotify.apollo.environment.IncomingRequestAwareClient;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

import okio.ByteString;

class ForwardingHttpClient implements IncomingRequestAwareClient {

  private final IncomingRequestAwareClient baseClient;
  private final IncomingRequestAwareClient httpClient;

  ForwardingHttpClient(IncomingRequestAwareClient baseClient,
                       IncomingRequestAwareClient httpClient) {
    this.baseClient = baseClient;
    this.httpClient = httpClient;
  }

  public static ForwardingHttpClient create(IncomingRequestAwareClient baseClient,
                                            IncomingRequestAwareClient httpClient) {
    return new ForwardingHttpClient(baseClient, httpClient);
  }

  @Override
  public CompletionStage<Response<ByteString>> send(
      Request request,
      Optional<Request> incoming) {
    if (request.uri().startsWith("http:") || request.uri().startsWith("https:")) {
      return httpClient.send(request, incoming);
    } else {
      return baseClient.send(request, incoming);
    }
  }
}
