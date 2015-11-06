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
