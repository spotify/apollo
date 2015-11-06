/*
 * Copyright (c) 2013 Spotify AB
 */

package com.spotify.apollo.dispatch;

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;

import com.spotify.apollo.RequestContext;
import com.spotify.apollo.request.OngoingRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletionException;

import static com.spotify.apollo.Response.forStatus;
import static com.spotify.apollo.Status.INTERNAL_SERVER_ERROR;

public class EndpointInvocationHandler {

  private static final Logger LOG = LoggerFactory.getLogger(EndpointInvocationHandler.class);

  private static final String ACCESS_POINT_REQUEST_SERVICE = "ap";
  private static final String ACCESS_POINT_ANONYMOUS_REQUEST_SERVICE = "ap/anonymous";
  private static final String WEBGATE_REQUEST_SERVICE = "webgate";

  /**
   * Fires off the request processing asynchronously - that is, this method is likely to return
   * before the request processing finishes.
   */
  public void handle(OngoingRequest ongoingRequest, RequestContext requestContext, Endpoint endpoint) {
    try {
      endpoint.invoke(requestContext)
          .whenComplete((message, throwable) -> {
            if (message != null) {
              ongoingRequest.reply(message);
            } else if (throwable != null) {
              // TODO: when are exceptions wrapped?
              // unwrap CompletionException
              if (throwable instanceof CompletionException) {
                throwable = throwable.getCause();
              }
              handleException(throwable, ongoingRequest);
            }
            else {
              LOG.error("Both message and throwable null in EndpointInvocationHandler for request " + ongoingRequest
                        + " - this shouldn't happen!");
              handleException(new IllegalStateException("Both message and throwable null"),
                              ongoingRequest);
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

    String service = ongoingRequest.request().service().orElse(null);
    // TODO this is shaky security-wise. this could already fall through in, for example, external
    // api calls, that do not go through the ap, ap-anonymous, or webgate.
    boolean isClientRequest = ACCESS_POINT_REQUEST_SERVICE.equals(service)
                              || ACCESS_POINT_ANONYMOUS_REQUEST_SERVICE.equals(service)
                              || WEBGATE_REQUEST_SERVICE.equals(service);

    if (!isClientRequest) {
      ongoingRequest.reply(forStatus(INTERNAL_SERVER_ERROR.withReasonPhrase(message)));
    } else {
      ongoingRequest.reply(forStatus(INTERNAL_SERVER_ERROR));
    }
  }
}
