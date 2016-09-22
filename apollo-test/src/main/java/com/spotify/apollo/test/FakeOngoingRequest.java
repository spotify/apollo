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

import com.spotify.apollo.Request;
import com.spotify.apollo.RequestMetadata;
import com.spotify.apollo.Response;
import com.spotify.apollo.request.OngoingRequest;
import com.spotify.apollo.request.RequestMetadataImpl;

import java.time.Instant;
import java.util.Optional;
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

  @Override
  public RequestMetadata metadata() {
    return RequestMetadataImpl.create(getClass(), Instant.EPOCH, "fake-request", Optional.empty(), Optional.empty());
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
