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
import com.spotify.apollo.Response;
import com.spotify.apollo.Status;
import com.spotify.apollo.StatusType;
import com.spotify.apollo.request.OngoingRequest;
import com.spotify.apollo.request.RequestHandler;
import com.spotify.apollo.request.ServerInfo;

import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
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

  private static final Logger LOGGER = LoggerFactory.getLogger(ApolloRequestHandler.class);

  private final RequestHandler requestHandler;
  private final ServerInfo serverInfo;
  private final Duration requestTimeout;

  ApolloRequestHandler(ServerInfo serverInfo, RequestHandler requestHandler,
                       Duration requestTimeout) {
    this.requestHandler = requireNonNull(requestHandler);
    this.serverInfo = requireNonNull(serverInfo);
    this.requestTimeout = requireNonNull(requestTimeout);
  }

  @Override
  public void handle(
      String target,
      org.eclipse.jetty.server.Request baseRequest,
      HttpServletRequest req,
      HttpServletResponse resp) throws IOException, ServletException {

    final long arrivalTimeNanos = System.nanoTime();
    final AsyncContext asyncContext = baseRequest.startAsync();
    asyncContext.setTimeout(requestTimeout.toMillis());
    asyncContext.addListener(TimeoutListener.getInstance());

    requestHandler.handle(new AsyncContextOngoingRequest(
        serverInfo, asApolloRequest(req), asyncContext, arrivalTimeNanos));

    baseRequest.setHandled(true);
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

  static class AsyncContextOngoingRequest implements OngoingRequest {

    private final ServerInfo serverInfo;
    private final long arrivalTimeNanos;
    private final Request request;
    private final AsyncContext asyncContext;

    AsyncContextOngoingRequest(ServerInfo serverInfo, Request request, AsyncContext asyncContext,
                               long arrivalTimeNanos) {
      this.serverInfo = serverInfo;
      this.request = requireNonNull(request);
      this.asyncContext = requireNonNull(asyncContext);
      this.arrivalTimeNanos = arrivalTimeNanos;
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
      final HttpServletResponse httpResponse = (HttpServletResponse) asyncContext.getResponse();

      final StatusType status = response.status();
      httpResponse.setStatus(status.code(), status.reasonPhrase());

      response.headers().asMap().forEach(httpResponse::addHeader);

      response.payload().ifPresent(payload -> {
        try {
          payload.write(httpResponse.getOutputStream());
        } catch (IOException e) {
          LOGGER.error("Failed to write response", e);
        }
      });

      asyncContext.complete();
    }

    @Override
    public void drop() {
      // 'true' dropping in the sense of dropping on the floor doesn't seem easily done with Jetty
      reply(Response.forStatus(Status.INTERNAL_SERVER_ERROR.withReasonPhrase("dropped")));
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

}
