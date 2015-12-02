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

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;

import com.spotify.apollo.Request;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import okio.ByteString;

import static java.util.Optional.of;

@AutoValue
abstract class HttpRequest implements Request {

  @Override
  public Request withUri(String uri) {
    return create(method(), uri, payload(), service(), parameters(), headers());
  }

  @Override
  public Request withService(String service) {
    return create(method(), uri(), payload(), of(service), parameters(), headers());
  }

  @Override
  public Request withHeader(String name, String value) {
    ImmutableMap.Builder<String, String> headers = ImmutableMap.builder();
    headers.putAll(headers());
    headers.put(name, value);
    return create(method(), uri(), payload(), service(), parameters(), headers.build());
  }

  @Override
  public Request withPayload(ByteString payload) {
    return create(method(), uri(), of(payload), service(), parameters(), headers());
  }

  public static Request create(
      String method,
      String uri,
      Optional<ByteString> payload,
      Optional<String> service,
      Map<String, List<String>> parameters,
      Map<String, String> headers) {
    return new AutoValue_HttpRequest(method, uri, parameters, headers, service, payload);
  }
}
