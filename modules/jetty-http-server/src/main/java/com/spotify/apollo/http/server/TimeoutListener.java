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

import java.io.IOException;

import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.http.HttpServletResponse;

/**
 * Handles timeouts sent by Jetty, ensuring we send a response.
 */
final class TimeoutListener implements AsyncListener {
  private static final TimeoutListener INSTANCE = new TimeoutListener();

  private TimeoutListener() {
    // prevent instantiation from outside class
  }

  @Override
  public void onComplete(AsyncEvent event) throws IOException {
    // empty
  }

  @Override
  public void onTimeout(AsyncEvent event) throws IOException {
    ((HttpServletResponse) event.getSuppliedResponse()).sendError(500, "Timeout");
    event.getAsyncContext().complete();
  }

  @Override
  public void onError(AsyncEvent event) throws IOException {
    // empty
  }

  @Override
  public void onStartAsync(AsyncEvent event) throws IOException {
    // empty
  }

  public static TimeoutListener getInstance() {
    return INSTANCE;
  }
}
