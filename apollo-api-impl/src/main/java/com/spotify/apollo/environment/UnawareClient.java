package com.spotify.apollo.environment;

import com.spotify.apollo.Client;
import com.spotify.apollo.Request;
import com.spotify.apollo.Response;

import java.io.IOException;
import java.util.concurrent.CompletionStage;

import okio.ByteString;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;

/**
 * An apollo service Client that uses a {@link IncomingRequestAwareClient} with no incoming
 * {@link Request}.
 */
class UnawareClient implements Client {

  private final IncomingRequestAwareClient delegate;

  UnawareClient(IncomingRequestAwareClient delegate) {
    this.delegate = requireNonNull(delegate);
  }

  @Override
  public CompletionStage<Response<ByteString>> send(Request request) {
    return delegate.send(request, empty());
  }
}
