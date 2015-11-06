package com.spotify.apollo.http.client;

import com.spotify.apollo.environment.ClientDecorator;
import com.spotify.apollo.environment.IncomingRequestAwareClient;

class HttpClientDecorator implements ClientDecorator {

  private static final HttpClientDecorator INSTANCE = new HttpClientDecorator();

  HttpClientDecorator() {
  }

  public static HttpClientDecorator create() {
    return INSTANCE;
  }

  @Override
  public IncomingRequestAwareClient apply(IncomingRequestAwareClient baseClient) {
    return ForwardingHttpClient.create(baseClient, HttpClient.create());
  }
}
