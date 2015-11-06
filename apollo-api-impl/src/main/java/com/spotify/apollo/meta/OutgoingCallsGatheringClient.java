package com.spotify.apollo.meta;

import com.spotify.apollo.Client;
import com.spotify.apollo.Request;
import com.spotify.apollo.Response;
import com.spotify.apollo.environment.IncomingRequestAwareClient;

import java.io.IOException;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

import okio.ByteString;

/**
 * An Apollo {@link Client} that gethers call info to an {@link OutgoingCallsGatherer}
 */
public class OutgoingCallsGatheringClient implements IncomingRequestAwareClient {

  private final OutgoingCallsGatherer callsGatherer;
  private final IncomingRequestAwareClient delegate;

  public OutgoingCallsGatheringClient(OutgoingCallsGatherer callsGatherer, IncomingRequestAwareClient delegate) {
    this.callsGatherer = Objects.requireNonNull(callsGatherer, "callsGatherer");
    this.delegate = Objects.requireNonNull(delegate, "delegate");
  }

  @Override
  public CompletionStage<Response<ByteString>> send(Request request, Optional<Request> incoming) {
    final URI uri = URI.create(request.uri());
    final String service = uri.getAuthority();
    callsGatherer.gatherOutgoingCall(service, request);

    return delegate.send(request, incoming);
  }
}
