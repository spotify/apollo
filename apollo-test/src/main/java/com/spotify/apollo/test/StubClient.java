/*
 * -\-\-
 * Spotify Apollo Testing Helpers
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
package com.spotify.apollo.test;

import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;

import com.spotify.apollo.Client;
import com.spotify.apollo.Request;
import com.spotify.apollo.Response;
import com.spotify.apollo.environment.IncomingRequestAwareClient;
import com.spotify.apollo.test.response.ResponseSource;
import com.spotify.apollo.test.response.ResponseWithDelay;
import com.spotify.apollo.test.response.Responses;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.io.Closeable;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import okio.ByteString;

import static java.util.stream.Collectors.toList;

/**
 * A stub (http://www.martinfowler.com/articles/mocksArentStubs.html) client that allows you to
 * preconfigure responses to certain messages, as well as verify whether expected messages are sent.
 * When evaluating how to respond, request/response mapping rules are evaluated in reverse order of
 * when they were added, allowing the 'latest' decision to override previous ones. Note that this
 * doesn't hold when using the deprecated {@link #respond(ResponseSource)} and
 * {@link #respond(Response)} methods to map responses to incoming requests - for those methods,
 * matchers are applied in the same order as they are added.
 *
 * Use the {@link #clear()} method to clear previous request/response mappings.
 */
public class StubClient implements Client, Closeable {

  private final boolean ownExecutor;
  private final ScheduledExecutorService executor;
  private final LinkedList<MatcherResponseSourcePair> mappings = new LinkedList<>();
  private final List<RequestResponsePair> requestsAndResponses;

  public StubClient() {
    this(Executors.newSingleThreadScheduledExecutor(), true);
  }

  public StubClient(ScheduledExecutorService executor) {
    this(executor, false);
  }

  private StubClient(ScheduledExecutorService executor, boolean ownExecutor) {
    this.ownExecutor = ownExecutor;
    this.executor = Objects.requireNonNull(executor);
    this.requestsAndResponses = Collections.synchronizedList(new LinkedList<>());
  }

  @Override
  public CompletionStage<Response<ByteString>> send(Request request) {
    final ResponseSource responseSource = responseSource(request);
    if (responseSource == null) {
      final NoMatchingResponseFoundException notFound =
          new NoMatchingResponseFoundException("Could not find any mapping for " + request.uri());
      final CompletableFuture<Response<ByteString>> notFoundFuture = new CompletableFuture<>();
      notFoundFuture.completeExceptionally(notFound);
      return notFoundFuture;
    }

    // Create response task
    final ResponseWithDelay responseWithDelay = responseSource.create(request);

    // Schedule a response in the future
    final CompletableFuture<Response<ByteString>> future = new CompletableFuture<>();
    final Runnable replyTask = () -> {
      Response<ByteString> response = responseWithDelay.getResponse();
      requestsAndResponses.add(RequestResponsePair.create(request, response));
      future.complete(response);
    };
    executor.schedule(replyTask, responseWithDelay.getDelayMillis(), TimeUnit.MILLISECONDS);

    return future;
  }

  /**
   * Set up a reaction to requests matching certain criteria. The supplied ResponseSource will
   * be invoked for each request that matches the supplied Matcher.
   */
  private void mapRequestToResponses(Matcher<Request> requestMatcher, ResponseSource responses) {
    mappings.add(MatcherResponseSourcePair.create(requestMatcher, responses));
  }

  /**
   * Adds the supplied matcher first in the list of matchers to consider when mapping requests
   * to responses.
   */
  private void addMatcherFirst(Matcher<Request> requestMatcher, ResponseSource responses) {
    mappings.addFirst(MatcherResponseSourcePair.create(requestMatcher, responses));
  }

  @Override
  public void close() {
    if (ownExecutor) {
      executor.shutdown();
    }
  }

  /**
   * @return An {@link IncomingRequestAwareClient} that ignores the incoming request
   */
  IncomingRequestAwareClient asRequestAwareClient() {
    return (request, ignored) -> StubClient.this.send(request);
  }

  /**
   * Find a response source that matches an incoming request.
   * @return a response source for this request, or null if none was found.
   */
  private ResponseSource responseSource(Request request) {
    for (MatcherResponseSourcePair entry : mappings) {
      if (entry.requestMatcher().matches(request)) {
        return entry.responseSource();
      }
    }
    return null;
  }

  /**
   * Create a request matcher that does a strict comparison by uri.
   */
  private static Matcher<Request> strictUriMatcher(String uri) {
    return new TypeSafeMatcher<Request>() {
      @Override
      protected boolean matchesSafely(Request request) {
        return uri.equals(request.uri());
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("with uri");
        description.appendValue(uri);
      }
    };
  }

  /**
   * Returns all the requests sent to this stub client.
   */
  public List<Request> sentRequests() {
    return requestsAndResponses.stream()
        .map(RequestResponsePair::request)
        .collect(toList());
  }

  /**
   * Clears the requests and responses tracked by this client.
   */
  public void clearRequests() {
    requestsAndResponses.clear();
  }

  /**
   * Returns all the requests sent to this stub client, together with their associated responses.
   */
  public List<RequestResponsePair> requestsAndResponses() {
    return requestsAndResponses;
  }

  /**
   * Clears the previously setup request to response mappings, but not the history of sent messages.
   */
  public void clear() {
    mappings.clear();
  }

  /**
   * Configure a constant (i.e., all matching requests will always result in the same response)
   * response for some request. The returned builder allows configuration of payload (default
   * no payload) and delay before the response is sent (default 0).
   *
   * When using this method to define request to response mappings, the mappings will be evaluated
   * in the order which they were added.
   * @deprecated in favour of {@link #when(Matcher)} or {@link #when(String)}.
   */
  @Deprecated
  public StubbedResponseBuilder respond(Response<ByteString> response) {
    return new StubbedResponseBuilder(ResponseWithDelay.forResponse(response));
  }

  /**
   * Configure a response source for matching requests. Each time a request is sent that matches
   * the to-be-specified criteria, the supplied ResponseSource will be invoked and its result
   * returned as a response.
   *
   * When using this method to define request to response mappings, the mappings will be evaluated
   * in the order which they were added.
   * @deprecated in favour of {@link #when(Matcher)} or {@link #when(String)}.
   */
  @Deprecated
  public StubbedResponseBuilder respond(ResponseSource responseSource) {
    return new StubbedResponseBuilder(responseSource);
  }

  /**
   * Configure a response source for matching requests. Each time a request is sent to the specified
   * URI (irrespective of the method used), the to-be-specified Response or ResponseSource will be
   * used.
   *
   * Request to response mappings defined using this method will be applied in reverse order of
   * addition, meaning that you can override previous choices in test code.
   */
  public WhenResponseBuilder when(String uri) {
    return new WhenResponseBuilder(strictUriMatcher(uri));
  }

  /**
   * Configure a response source for matching requests. Each time a request is sent that matches
   * the supplied matcher, the to-be-specified Response or ResponseSource will be used.
   *
   * Request to response mappings defined using this method will be applied in reverse order of
   * addition, meaning that you can override previous choices in test code.
   */
  public WhenResponseBuilder when(Matcher<Request> requestMatcher) {
    return new WhenResponseBuilder(requestMatcher);
  }

  public static final class NoMatchingResponseFoundException extends Exception {
    public NoMatchingResponseFoundException(String message) {
      super(message);
    }
  }

  /**
   * Immutable response builder.
   */
  public class StubbedResponseBuilder {
    @Nullable
    private final ResponseWithDelay responseWithDelay;
    @Nullable
    private final ResponseSource responseSource;

    private StubbedResponseBuilder(ResponseWithDelay responseWithDelay) {
      Preconditions.checkNotNull(responseWithDelay);
      this.responseWithDelay = responseWithDelay;
      this.responseSource = null;
    }

    private StubbedResponseBuilder(ResponseSource responseSource) {
      Preconditions.checkNotNull(responseSource);
      this.responseWithDelay = null;
      this.responseSource = responseSource;
    }

    /**
     * Configure delay before the response should be sent.
     */
    public StubbedResponseBuilder in(long time, TimeUnit unit) {
      Preconditions.checkState(
          responseWithDelay != null,
          "method not available when using a ResponseSource");

      return new StubbedResponseBuilder(
          ResponseWithDelay.forResponse(responseWithDelay.getResponse(), time, unit));
    }

    /**
     * Map the previously configured response or response source to a URI. This means any invocation
     * of that URI, no matter which request method is used, will lead to a match.
     */
    public void to(String uri) {
      mapRequestToResponses(strictUriMatcher(uri), responseSource());
    }

    /**
     * Map the previously configured response or response source to any messages matching the
     * supplied {@link Matcher}. This gives full freedom to do things like inspect payloads,
     * headers, etc., to match requests.
     */
    public void to(Matcher<Request> requestMatcher) {
      mapRequestToResponses(requestMatcher, responseSource());
    }

    private ResponseSource responseSource() {
      if (responseSource != null) {
        return responseSource;
      }

      // should be checked by preconditions
      Preconditions.checkNotNull(responseWithDelay);
      return Responses.constant(responseWithDelay);
    }

  }


  public class WhenResponseBuilder {

    private final Matcher<Request> requestMatcher;

    public WhenResponseBuilder(Matcher<Request> requestMatcher) {
      this.requestMatcher = Objects.requireNonNull(requestMatcher);
    }

    /**
     * Immediately respond with the supplied response to any matching request.
     */
    public void respond(Response<ByteString> response) {
      addMatcherFirst(requestMatcher, Responses.constant(ResponseWithDelay.forResponse(response)));
    }

    /**
     * Respond with responses and delays supplied by the provided ResponseSource to any matching
     * request.
     */
    public void respond(ResponseSource responseSource) {
      addMatcherFirst(requestMatcher, responseSource);
    }
  }

  @AutoValue
  static abstract class RequestResponsePair {
    public abstract Request request();
    public abstract Response<ByteString> response();

    public static RequestResponsePair create(Request request, Response<ByteString> response) {
      return new AutoValue_StubClient_RequestResponsePair(request, response);
    }
  }

  @AutoValue
  static abstract class MatcherResponseSourcePair {
    public abstract Matcher<Request> requestMatcher();
    public abstract ResponseSource responseSource();

    public static MatcherResponseSourcePair create(Matcher<Request> requestMatcher, ResponseSource responseSource) {
      return new AutoValue_StubClient_MatcherResponseSourcePair(requestMatcher, responseSource);
    }
  }
}
