/*
 * -\-\-
 * Spotify Apollo Jetty HTTP Server Module
 * --
 * Copyright (C) 2013 - 2016 Spotify AB
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
package com.spotify.apollo.http.server;

import com.spotify.apollo.Request;
import com.spotify.apollo.Response;
import com.spotify.apollo.Status;
import com.spotify.apollo.StatusType;
import com.spotify.apollo.request.OngoingRequest;
import com.spotify.apollo.request.ServerInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletResponse;

import okio.ByteString;

import static java.util.Objects.requireNonNull;

/**
 * OngoingRequest responding via an underlying AsyncContext.
 */
class AsyncContextOngoingRequest implements OngoingRequest {

  private static final Logger LOGGER = LoggerFactory.getLogger(AsyncContextOngoingRequest.class);

  private final ServerInfo serverInfo;
  private final long arrivalTimeNanos;
  private final Request request;
  private final AsyncContext asyncContext;
  private final RequestOutcomeConsumer logger;
  private final AtomicBoolean replied = new AtomicBoolean(false);

  AsyncContextOngoingRequest(ServerInfo serverInfo, Request request, AsyncContext asyncContext,
                             long arrivalTimeNanos, RequestOutcomeConsumer logger) {
    this.serverInfo = serverInfo;
    this.request = requireNonNull(request);
    this.asyncContext = requireNonNull(asyncContext);
    this.arrivalTimeNanos = arrivalTimeNanos;
    this.logger = requireNonNull(logger);
  }

  @Override
  public Request request() {
    return request;
  }

  @Override
  public ServerInfo serverInfo() {
    return serverInfo;
  }

  @Override
  public void reply(Response<ByteString> response) {
    sendReply(response);
  }

  @Override
  public void drop() {
    // 'true' dropping in the sense of dropping on the floor doesn't seem easily done with Jetty
    sendReply(Response.forStatus(Status.INTERNAL_SERVER_ERROR.withReasonPhrase("dropped")));
  }

  // handles common functionality for reply() and drop() and is not overridable
  private void sendReply(Response<ByteString> response) {
    if (!replied.compareAndSet(false, true)) {
      LOGGER.warn("Already replied to ongoing request {} - spurious response {}", request, response);
    } else {
      final HttpServletResponse httpResponse = (HttpServletResponse) asyncContext.getResponse();

      final StatusType status = response.status();
      httpResponse.setStatus(status.code(), status.reasonPhrase());

      response.headers().asMap().forEach(httpResponse::addHeader);

      response.payload().ifPresent(payload -> {
        try {
          payload.write(httpResponse.getOutputStream());
        } catch (IOException e) {
          LOGGER.warn("Failed to write response", e);
        }
      });

      asyncContext.complete();

      logger.accept(this, Optional.of(response));
    }
  }

  @Override
  public boolean isExpired() {
    return false;
  }

  @Override
  public long arrivalTimeNanos() {
    return arrivalTimeNanos;
  }
}
