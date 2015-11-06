/*
 * Copyright (c) 2013-2014 Spotify AB
 */

package com.spotify.apollo.test;

import com.spotify.apollo.Request;
import com.spotify.apollo.Response;
import com.spotify.apollo.request.OngoingRequest;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import okio.ByteString;

/**
 * A mock service request used when injecting a request into an Apollo application.
 */
public class FakeOngoingRequest implements OngoingRequest {

  private final Request request;
  private final CompletableFuture<Response<ByteString>> reply = new CompletableFuture<>();

  /**
   * Create a new mock service request holding an Apollo {@link Request}.
   *
   * @param request The request.
   */
  public FakeOngoingRequest(Request request) {
    this.request = request;
  }

  @Override
  public Request request() {
    return request;
  }

  @Override
  public void reply(Response<ByteString> response) {
    reply.complete(response);
  }

  @Override
  public void drop() {
    reply.completeExceptionally(new Throwable("dropped"));
  }

  @Override
  public boolean isExpired() {
    return false;
  }

  /**
   * Get a future holding the reply.
   *
   * @return A {@link CompletionStage} holding the reply.
   */
  public CompletionStage<Response<ByteString>> getReply() {
    return reply;
  }
}
