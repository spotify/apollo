/*
 * -\-\-
 * Spotify Apollo API Environment
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
package com.spotify.apollo.environment;

import com.spotify.apollo.Request;
import com.spotify.apollo.Response;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

import okio.ByteString;

import static java.util.Objects.requireNonNull;

/**
 * A {@link IncomingRequestAwareClient} that ensures the {@link Request#service()} value is set
 * on all requests.
 */
class ServiceSettingClient implements IncomingRequestAwareClient {

  private final String serviceName;
  private final IncomingRequestAwareClient delegate;

  ServiceSettingClient(String serviceName, IncomingRequestAwareClient delegate) {
    this.serviceName = requireNonNull(serviceName);
    this.delegate = requireNonNull(delegate);
  }

  @Override
  public CompletionStage<Response<ByteString>> send(Request request, Optional<Request> incoming) {
    final Request withService = !request.service().isPresent()
        ? request.withService(serviceName)
        : request;

    return delegate.send(withService, incoming);
  }
}
