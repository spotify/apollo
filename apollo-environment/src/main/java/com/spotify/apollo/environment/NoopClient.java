package com.spotify.apollo.environment;

import com.spotify.apollo.Request;
import com.spotify.apollo.Response;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

import okio.ByteString;

/**
 * An Apollo {@link IncomingRequestAwareClient} that throws {@link UnsupportedOperationException}
 */
class NoopClient implements IncomingRequestAwareClient {

  @Override
  public CompletionStage<Response<ByteString>> send(Request request, Optional<Request> incoming) {
    throw new UnsupportedOperationException();
  }
}
