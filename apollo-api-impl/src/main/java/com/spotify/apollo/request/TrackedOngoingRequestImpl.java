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

import com.spotify.apollo.Response;

import okio.ByteString;

@Deprecated
class TrackedOngoingRequestImpl extends ForwardingOngoingRequest {

  private final RequestTracker requestTracker;

  TrackedOngoingRequestImpl(OngoingRequest ongoingRequest, RequestTracker requestTracker) {
    super(ongoingRequest);
    this.requestTracker = requestTracker;

    requestTracker.register(this);
  }

  @Override
  public void reply(Response<ByteString> message) {
    doReply(message);
  }

  @Override
  public void drop() {
    final boolean removed = requestTracker.remove(this);
    if (removed) {
      super.drop();
    }
  }

  private boolean doReply(Response<ByteString> message) {
    final boolean removed = requestTracker.remove(this);
    if (removed) {
      super.reply(message);
    }

    return removed;
  }
}
