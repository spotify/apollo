/*
 * Copyright (c) 2013-2014 Spotify AB
 */

package com.spotify.apollo.route;

import com.spotify.apollo.RequestContext;
import com.spotify.apollo.Response;

/**
 * Synchronous endpoint handler. Depending on the response type, Apollo will act differently.
 * Return a {@link Response} when you want to do things like modify response headers based on
 * results of service invocations or control the status code returned. Examples:
 *
 * <code>
 *   public Response<String> invoke(RequestContext requestContext) {
 *     String s = stringResponse();
 *     return Response.forPayload(s)
 *         .withHeader("X-Payload-Length", s.length());
 *   }
 * </code>
 *
 * <code>
 *   public Response<String> invoke(RequestContext requestContext) {
 *     Map<String, String> args = requestContext.pathArgs();
 *     if (args.get("arg") == null || args.get("arg").isEmpty()) {
 *       return Response.forStatus(Status.BAD_REQUEST
 *           .withReasonPhrase("Mandatory argument 'arg' missing"));
 *     }
 *
 *     return Response.forPayload(s);
 *   }
 * </code>
 *
 * Any other return type will be serialized with the configured serializer and added as payload to
 * a response with status {@link com.spotify.apollo.Status#OK}.
 *
 * @param <T>
 */
@FunctionalInterface
public interface SyncHandler<T> {
  T invoke(RequestContext requestContext);
}
