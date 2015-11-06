/**
 * Copyright (C) 2013 Spotify AB
 */
package com.spotify.apollo.environment;

import com.spotify.apollo.Client;
import com.spotify.apollo.Request;
import com.spotify.apollo.Response;
import com.spotify.apollo.environment.IncomingRequestAwareClient;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

import okio.ByteString;

class RequestScopedClient implements Client {

  private final IncomingRequestAwareClient delegate;
  private final Request origin;

  RequestScopedClient(IncomingRequestAwareClient delegate, Request origin) {
    this.delegate = delegate;
    this.origin = origin;
  }

  @Override
  public CompletionStage<Response<ByteString>> send(Request request) {
    return delegate.send(request, Optional.of(origin));
  }
}
