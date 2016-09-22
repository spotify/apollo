/*
 * -\-\-
 * Spotify Apollo Jetty HTTP Server Module
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
package com.spotify.apollo.http.server;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;

import com.spotify.apollo.Request;
import com.spotify.apollo.RequestMetadata;
import com.spotify.apollo.request.RequestHandler;
import com.spotify.apollo.request.RequestMetadataImpl;

import org.eclipse.jetty.server.handler.AbstractHandler;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import okio.ByteString;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;

class ApolloRequestHandler extends AbstractHandler {

  private final RequestHandler requestHandler;
  private final RequestMetadata.HostAndPort serverInfo;
  private final Duration requestTimeout;
  private final RequestOutcomeConsumer logger;

  ApolloRequestHandler(RequestMetadata.HostAndPort serverInfo, RequestHandler requestHandler,
                       Duration requestTimeout, RequestOutcomeConsumer logger) {
    this.requestHandler = requireNonNull(requestHandler);
    this.serverInfo = requireNonNull(serverInfo);
    this.requestTimeout = requireNonNull(requestTimeout);
    this.logger = requireNonNull(logger);
  }

  @Override
  public void handle(
      String target,
      org.eclipse.jetty.server.Request baseRequest,
      HttpServletRequest req,
      HttpServletResponse resp) throws IOException, ServletException {

    final AsyncContext asyncContext = baseRequest.startAsync();

    RequestMetadata metadata = extractMetadata(req);

    AsyncContextOngoingRequest ongoingRequest =
        new AsyncContextOngoingRequest(asApolloRequest(req),
                                       asyncContext,
                                       logger,
                                       metadata);

    asyncContext.setTimeout(requestTimeout.toMillis());
    asyncContext.addListener(TimeoutListener.create(ongoingRequest));

    requestHandler.handle(ongoingRequest);

    baseRequest.setHandled(true);
  }

  private RequestMetadata extractMetadata(HttpServletRequest req) {
    return RequestMetadataImpl.create(
        getClass(),
        Instant.now(),
        req.getProtocol(),
        Optional.of(serverInfo),
        Optional.of(RequestMetadataImpl.hostAndPort(req.getRemoteHost(), req.getRemotePort())));
  }

  @VisibleForTesting
  Request asApolloRequest(HttpServletRequest req) throws IOException {
    final String uri = req.getRequestURI() +
                       (req.getQueryString() == null ? "" : "?" + req.getQueryString());
    final String method = req.getMethod();
    final int contentLength = req.getContentLength();

    final Optional<ByteString> payload = (contentLength > -1)
        ? of(readPayload(req, contentLength))
        : empty();

    final ImmutableMap.Builder<String, String> headersBuilder = ImmutableMap.builder();
    toStream(req.getHeaderNames())
        .forEachOrdered(
            name -> headersBuilder.put(
                name, toStream(req.getHeaders(name)).collect(Collectors.joining(","))
            ));

    final ImmutableMap<String, String> headers = headersBuilder.build();

    Request result = Request.forUri(uri, method)
        .withHeaders(headers);

    final String callingService = headers.get("X-Calling-Service");
    if (!isNullOrEmpty(callingService)) {
      result = result.withService(callingService);
    }

    if (payload.isPresent()) {
      result = result.withPayload(payload.get());
    }

    return result;
  }

  private ByteString readPayload(HttpServletRequest req, int contentLength) throws IOException {
    final InputStream input = new BufferedInputStream(req.getInputStream());
    return ByteString.read(input, contentLength);
  }

  private static <T> Stream<T> toStream(Enumeration<T> enumeration) {
    List<T> list = new ArrayList<>();
    while (enumeration.hasMoreElements()) {
      list.add(enumeration.nextElement());
    }
    return list.stream();
  }
}
