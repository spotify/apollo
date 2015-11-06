package com.spotify.apollo.http.server;

import com.spotify.apollo.request.RequestHandler;

import java.io.Closeable;

/**
 * A fully configured server that can be started and stopped.
 */
public interface HttpServer extends Closeable {

  /**
   * Start the server using the given {@link RequestHandler}.
   *
   * @param requestHandler  The request handler that should handle server requests
   */
  void start(RequestHandler requestHandler);
}
