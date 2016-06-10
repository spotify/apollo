package com.spotify.apollo.httpservice;


import com.spotify.apollo.environment.ApolloConfig;
import com.spotify.apollo.http.client.OkHttpClientConfiguration;
import com.spotify.apollo.http.server.JettyHttpServerConfiguration;

/**
 * TODO: document!
 */
public class HttpServiceConfiguration {
  public ApolloConfig apollo;
  public JettyHttpServerConfiguration httpServer;
  public OkHttpClientConfiguration httpClient;
}
