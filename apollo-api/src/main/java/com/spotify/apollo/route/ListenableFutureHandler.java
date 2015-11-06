/*
 * Copyright (c) 2013-2014 Spotify AB
 */

package com.spotify.apollo.route;

import com.google.common.util.concurrent.ListenableFuture;

import com.spotify.apollo.RequestContext;
import com.spotify.apollo.Response;

/**
 * Asynchronous endpoint handler. Depending on the response type, Apollo will act differently.
 * Return a future to a {@link Response} when you want to do things like modify response headers
 * based on results of service invocations or if you want to set the status code. Example:
 *
 * <code>
 *   public ListenableFuture<Response<String>> invoke(RequestContext requestContest) {
 *     return Futures.transform(futureToStringPayload(),
 *         (String s) -> {
 *            return Response.forPayload(s)
 *                .withHeader("X-Payload-Length", s.length());
 *         });
 *   }
 * </code>
 *
 * Any other return type will be serialized with the configured serializer and added as payload to
 * a response with status {@link com.spotify.apollo.Status#OK}.
 *
 * @param <T>
 */
@FunctionalInterface
public interface ListenableFutureHandler<T> {
  ListenableFuture<T> invoke(RequestContext requestContext);
}
