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

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;

import com.spotify.apollo.Client;
import com.spotify.apollo.Request;
import com.spotify.apollo.RequestContext;

import java.util.Map;

@AutoValue
public abstract class RequestContexts implements RequestContext {

  public static RequestContext create(
      Request request, Client client, Map<String, String> pathArgs) {
    return create(request, client, pathArgs, System.nanoTime());
  }

  public static RequestContext create(
      Request request, Client client, Map<String, String> pathArgs, long arrivalTimeNanos) {
    return create(request, client, pathArgs, arrivalTimeNanos, ImmutableMap.of());
  }

  public static RequestContext create(
      Request request, Client client, Map<String, String> pathArgs,
      long arrivalTimeNanos, Map<String, String> metadata) {
    return new AutoValue_RequestContexts(request, client, pathArgs, arrivalTimeNanos, metadata);
  }

  // override default implementation to ensure auto-value will generate a field for this
  @Override
  public abstract long arrivalTimeNanos();

  // override default implementation to ensure auto-value will generate a field for this
  @Override
  public abstract Map<String, String> metadata();
}
