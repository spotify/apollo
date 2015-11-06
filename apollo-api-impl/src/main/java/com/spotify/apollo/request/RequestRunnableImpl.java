/**
 * Copyright (C) 2013 Spotify AB
 */

package com.spotify.apollo.request;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;

import com.spotify.apollo.Request;
import com.spotify.apollo.Response;
import com.spotify.apollo.StatusType;
import com.spotify.apollo.dispatch.Endpoint;
import com.spotify.apollo.route.ApplicationRouter;
import com.spotify.apollo.route.InvalidUriException;
import com.spotify.apollo.route.RuleMatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Optional;
import java.util.function.BiConsumer;

import okio.ByteString;

import static com.spotify.apollo.Response.forStatus;
import static com.spotify.apollo.Status.BAD_REQUEST;
import static com.spotify.apollo.Status.INTERNAL_SERVER_ERROR;
import static com.spotify.apollo.Status.METHOD_NOT_ALLOWED;
import static com.spotify.apollo.Status.NOT_FOUND;
import static com.spotify.apollo.Status.NO_CONTENT;

/**
 * Runs a request in an application and allows failing the request.
 */
class RequestRunnableImpl implements RequestRunnable {

  private static final Logger LOG = LoggerFactory.getLogger(RequestRunnableImpl.class);

  private final OngoingRequest ongoingRequest;
  private final ApplicationRouter<Endpoint> applicationRouter;

  RequestRunnableImpl(OngoingRequest ongoingRequest, ApplicationRouter<Endpoint> applicationRouter) {
    this.ongoingRequest = ongoingRequest;
    this.applicationRouter = applicationRouter;
  }

  /**
   * Route the request to a matching endpoint and continue with the {@link BiConsumer}
   */
  @Override
  public void run(BiConsumer<OngoingRequest, RuleMatch<Endpoint>> matchContinuation) {
    try {
      matchAndRun(matchContinuation);
    } catch (Exception e) {
      LOG.error("Exception when handling request", e);
      // ensure that we reply with a server error, if possible
      ongoingRequest.reply(forStatus(INTERNAL_SERVER_ERROR));
    }
  }

  private void matchAndRun(BiConsumer<OngoingRequest, RuleMatch<Endpoint>> matchContinuation) {
    final Request request = ongoingRequest.request();
    final Optional<RuleMatch<Endpoint>> match;

    try {
      match = applicationRouter.match(request);
    } catch (InvalidUriException e) {
      LOG.warn("bad uri {} {} {} {}", request.method(), request.uri(),
               messageId(request), BAD_REQUEST, e);
      ongoingRequest.reply(forStatus(BAD_REQUEST));
      return;
    }

    if (!match.isPresent()) {
      Collection<String> methods = applicationRouter.getMethodsForValidRules(request);
      if (methods.isEmpty()) {
        LOG.warn("not found {} {} {} {}", request.method(), request.uri(),
                 messageId(request), NOT_FOUND);
        ongoingRequest.reply(forStatus(NOT_FOUND));
      } else {
        StatusType statusCode;
        if ("OPTIONS".equals(request.method())) {
          statusCode = NO_CONTENT;
        } else {
          statusCode = METHOD_NOT_ALLOWED;
          LOG.warn("wrong method {} {} {} {}", request.method(), request.uri(),
                   messageId(request), statusCode);
        }
        methods = Sets.newTreeSet(methods);
        methods.add("OPTIONS");
        ongoingRequest.reply(
            Response.<ByteString>forStatus(statusCode)
                .withHeader("Allow", Joiner.on(", ").join(methods)));
      }
      return;
    }

    matchContinuation.accept(ongoingRequest, match.get());
  }

  // TODO implement
  private Optional<String> messageId(Request request) {
    return null;
  }
}
