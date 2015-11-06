/*
 * Copyright (c) 2013-2015 Spotify AB
 */

package com.spotify.apollo.request;

import com.spotify.apollo.Client;
import com.spotify.apollo.RequestContext;
import com.spotify.apollo.Response;
import com.spotify.apollo.dispatch.Endpoint;
import com.spotify.apollo.environment.IncomingRequestAwareClient;
import com.spotify.apollo.route.RuleMatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static com.spotify.apollo.Response.forStatus;
import static com.spotify.apollo.Status.INTERNAL_SERVER_ERROR;

/**
 * 1. run route matching {@link RequestRunnable}, continue with a {@link Endpoint}
 * 2. run matched route endpoint invocation
 * 2.1. endpoint does downstream calls
 * 2.2. downstream calls return
 * 3. endpoint invocation returns a {@link Response}
 * 4. reply on incoming {@link OngoingRequest}
 */
class RequestHandlerImpl implements RequestHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(RequestHandlerImpl.class);

  private final RequestRunnableFactory rrf;
  private final EndpointRunnableFactory erf;
  private final IncomingRequestAwareClient client;

  RequestHandlerImpl(
      RequestRunnableFactory requestRunnableFactory,
      EndpointRunnableFactory endpointRunnableFactory,
      IncomingRequestAwareClient client) {
    this.rrf = requestRunnableFactory;
    this.erf = endpointRunnableFactory;
    this.client = client;
  }

  @Override
  public void handle(OngoingRequest ongoingRequest) {
    try {
      rrf.create(ongoingRequest).run(this::handleEndpointMatch);
    }
    catch (Exception e) {
      LOGGER.error("Request matching/handling threw exception", e);
      try {
        ongoingRequest.reply(forStatus(INTERNAL_SERVER_ERROR));
      } catch (Throwable t) {
        LOGGER.error("Caught throwable when replying with Internal Server Error", t);
      }
    }
  }

  /**
   * Continuation for the {@link RequestRunnableFactory}
   *
   * @param request  request being processed
   * @param match    the match that was made
   */
  private void handleEndpointMatch(OngoingRequest request, RuleMatch<Endpoint> match) {
    final Endpoint endpoint = match.getRule().getTarget();
    final Map<String, String> parsedPathArguments = match.parsedPathArguments();
    final Client requestScopedClient = client.wrapRequest(request.request());
    final RequestContext requestContext =
        RequestContexts.create(request.request(), requestScopedClient, parsedPathArguments);

    erf.create(request, requestContext, endpoint)
        .run();
  }
}
