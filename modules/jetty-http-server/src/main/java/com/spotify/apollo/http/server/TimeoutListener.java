/*
 * -\-\-
 * Spotify Apollo Jetty HTTP Server Module
 * --
 * Copyright (C) 2013 - 2016 Spotify AB
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

import com.spotify.apollo.Response;
import com.spotify.apollo.Status;
import com.spotify.apollo.request.OngoingRequest;

import java.io.IOException;

import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;

import okio.ByteString;

import static java.util.Objects.requireNonNull;

/**
 * Handles timeouts sent by Jetty, ensuring we send a response.
 */
final class TimeoutListener implements AsyncListener {

  private static final Response<ByteString> TIMEOUT_RESPONSE =
      Response.forStatus(Status.INTERNAL_SERVER_ERROR.withReasonPhrase("Timeout"));
  private final OngoingRequest ongoingRequest;

  private TimeoutListener(OngoingRequest ongoingRequest) {
    this.ongoingRequest = requireNonNull(ongoingRequest);
  }

  @Override
  public void onComplete(AsyncEvent event) throws IOException {
    // empty
  }

  @Override
  public void onTimeout(AsyncEvent event) throws IOException {
    ongoingRequest.reply(TIMEOUT_RESPONSE);
  }

  @Override
  public void onError(AsyncEvent event) throws IOException {
    // empty
  }

  @Override
  public void onStartAsync(AsyncEvent event) throws IOException {
    // empty
  }

  public static TimeoutListener create(OngoingRequest ongoingRequest) {
    return new TimeoutListener(ongoingRequest);
  }
}
