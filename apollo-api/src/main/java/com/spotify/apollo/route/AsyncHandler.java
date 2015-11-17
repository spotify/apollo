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
