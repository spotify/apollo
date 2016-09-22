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
package com.spotify.apollo.request;

import com.spotify.apollo.Request;
import com.spotify.apollo.RequestMetadata;
import com.spotify.apollo.Response;

import java.util.Objects;

import okio.ByteString;

/**
 * A delegating implementation of {@link OngoingRequest} useful for implementing decorators.
 */
public abstract class ForwardingOngoingRequest implements OngoingRequest {

  private final OngoingRequest delegate;

  protected ForwardingOngoingRequest(OngoingRequest delegate) {
    this.delegate = Objects.requireNonNull(delegate, "delegate");
  }

  @Override
  public Request request() {
    return delegate.request();
  }

  @Override
  public void reply(Response<ByteString> response) {
    delegate.reply(response);
  }

  @Override
  public void drop() {
    delegate.drop();
  }

  @Override
  public boolean isExpired() {
    return delegate.isExpired();
  }

  @Override
  public RequestMetadata metadata() {
    return delegate.metadata();
  }
}
