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
