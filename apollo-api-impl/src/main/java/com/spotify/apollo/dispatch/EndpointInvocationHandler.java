/*
 * -\-\-
 * Spotify Apollo API Implementations
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
package com.spotify.apollo.dispatch;

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;

import com.spotify.apollo.RequestContext;
import com.spotify.apollo.request.EndpointRunnableFactory;
import com.spotify.apollo.request.OngoingRequest;

import okio.ByteString;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletionException;

import static com.spotify.apollo.Response.forStatus;
import static com.spotify.apollo.Status.INTERNAL_SERVER_ERROR;

public class EndpointInvocationHandler implements EndpointRunnableFactory {

  private static final Logger LOG = LoggerFactory.getLogger(EndpointInvocationHandler.class);

  @Override
  public Runnable create(
      OngoingRequest ongoingRequest,
      RequestContext requestContext,
      Endpoint endpoint) {

    return () -> handle(ongoingRequest, requestContext, endpoint);
  }

  /**
   * Fires off the request processing asynchronously - that is, this method is likely to return
   * before the request processing finishes.
   */
  void handle(OngoingRequest ongoingRequest, RequestContext requestContext, Endpoint endpoint) {
    try {
      endpoint.invoke(requestContext)
          .whenComplete((message, throwable) -> {
            try {
              if (message != null) {
                ongoingRequest.reply(message);
              } else if (throwable != null) {
                // unwrap CompletionException
                if (throwable instanceof CompletionException) {
                  throwable = throwable.getCause();
                }
                handleException(throwable, ongoingRequest);
              } else {
                LOG.error(
                    "Both message and throwable null in EndpointInvocationHandler for request "
                    + ongoingRequest
                    + " - this shouldn't happen!");
                handleException(new IllegalStateException("Both message and throwable null"),
                                ongoingRequest);
              }
            } catch (Throwable t) {
              // don't try to respond here; just log the fact that responding failed.
              LOG.error("Exception caught when replying", t);
            }
          });
    } catch (Exception e) {
      handleException(e, ongoingRequest);
    }
  }

  private static void handleException(Throwable e, OngoingRequest ongoingRequest) {
    String message = e.getMessage();
    message = !Strings.isNullOrEmpty(message) ? ": \"" + message + "\"" : "";
    message = CharMatcher.anyOf("\n\r").replaceFrom(message, ' ');

    LOG.warn("Got Exception {} when invoking endpoint for request: {}",
             message, ongoingRequest.request(), e);

    ongoingRequest.reply(
        forStatus(INTERNAL_SERVER_ERROR).withPayload(ByteString.encodeUtf8(message)));
  }
}
