/*
 * Copyright (c) 2013-2014 Spotify AB
 */

package com.spotify.apollo.route;

import com.spotify.apollo.RequestContext;
import com.spotify.apollo.Response;

import java.util.concurrent.CompletionStage;

/**
 * Asynchronous endpoint handler. Depending on the response type, Apollo will act differently.
 * Return a stage with a {@link Response} value when you want to do things like modify response
 * headers based on results of service invocations or if you want to set the status code. Example:
 *
 * <code>
 *   public CompletionStage<Response<String>> invoke(RequestContext requestContest) {
 *     return futureToStringPayload().thenApply(
 *         s -> Response.forPayload(s)
 *                .withHeader("X-Payload-Length", s.length()));
 *   }
 * </code>
 *
 * Any other return type will be serialized with the configured serializer and added as payload to
 * a response with status {@link com.spotify.apollo.Status#OK}.
 *
 * @param <T>
 */
@FunctionalInterface
public interface AsyncHandler<T> {
  CompletionStage<T> invoke(RequestContext requestContext);
}
