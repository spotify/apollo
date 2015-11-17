/*
 * Copyright (c) 2013-2014 Spotify AB
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
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import okio.ByteString;

import static com.google.common.collect.Maps.newLinkedHashMap;
import static java.util.stream.Collectors.toList;

/**
 * A stub (http://www.martinfowler.com/articles/mocksArentStubs.html) client that allows you to
 * preconfigure responses to certain messages, as well as verify whether expected messages are sent.
 * When evaluating how to respond, request/response mapping rules are evaluated in the order they
 * were added. Use the {@link #clear()} method to clear previous request/response mappings.
 */
public class StubClient implements Client, Closeable {

  private final boolean ownExecutor;
  private final ScheduledExecutorService executor;
  private final Map<Matcher<Request>, ResponseSource> mappings = newLinkedHashMap();
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
    mappings.put(requestMatcher, responses);
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
    for (Map.Entry<Matcher<Request>, ResponseSource> entry : mappings.entrySet()) {
      if (entry.getKey().matches(request)) {
        return entry.getValue();
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
   */
  public StubbedResponseBuilder respond(Response<ByteString> response) {
    return new StubbedResponseBuilder(ResponseWithDelay.forResponse(response));
  }

  /**
   * Configure a response source for matching requests. Each time a request is sent that matches
   * the to-be-specified criteria, the supplied ResponseSource will be invoked and its result
   * returned as a response.
   */
  public StubbedResponseBuilder respond(ResponseSource responseSource) {
    return new StubbedResponseBuilder(responseSource);
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

  @AutoValue
  public static abstract class RequestResponsePair {
    public abstract Request request();
    public abstract Response<ByteString> response();

    public static RequestResponsePair create(Request request, Response<ByteString> response) {
      return new AutoValue_StubClient_RequestResponsePair(request, response);
    }
  }
}
