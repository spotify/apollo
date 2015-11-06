package com.spotify.apollo;

import java.io.Closeable;
import java.util.concurrent.CompletionStage;

import okio.ByteString;

/**
 * An Apollo service client.
 *
 * Clients are available from:
 * - {@link RequestContext#requestScopedClient()} within request handling, or
 * - {@link Environment#client()} otherwise.
 *
 */
public interface Client {

  /**
   * Send a Request and get an asynchronous Response as a CompletionStage.
   *
   * @param request  the request to send
   * @return a CompletionStage that completes normally with a {@link Response<ByteString>},
   *     or completes exceptionally if there is a failure sending the request.
   *     An error status code returned by the service is a normal completion.
   */
  CompletionStage<Response<ByteString>> send(Request request);
}
