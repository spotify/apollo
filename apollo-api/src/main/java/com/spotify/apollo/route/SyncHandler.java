/*
 * -\-\-
 * Spotify Apollo API Interfaces
 * --
 * Copyright (C) 2013 - 2015 Spotify AB
 * --
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -/-/-
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
