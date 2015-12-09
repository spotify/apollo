/*
 * -\-\-
 * Spotify Apollo API Interfaces
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
package com.spotify.apollo;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import okio.ByteString;

import static java.util.Collections.emptyMap;
import static java.util.Optional.empty;
import static java.util.Optional.of;

/**
 * Request implementation
 */
@AutoValue
abstract class RequestValue implements Request {

  public static final String GET = "GET";

  public static Request create(String uri) {
    return create(uri, GET);
  }

  public static Request create(String uri, String method) {
    return create(method, uri, parseParameters(uri), emptyMap(), empty(), empty(), empty());
  }

  private static Request create(
      String method,
      String uri,
      Map<String, List<String>> parameters,
      Map<String, String> headers,
      Optional<String> service,
      Optional<ByteString> payload,
      Optional<Long> ttl) {
    return new AutoValue_RequestValue(
        method, uri,
        ImmutableMap.copyOf(parameters),
        ImmutableMap.copyOf(headers),
        service,
        payload,
        ttl);
  }

  @Override
  public Request withUri(String uri) {
    return create(method(), uri, parameters(), headers(), service(), payload(), ttl());
  }

  @Override
  public Request withService(String service) {
    return create(method(), uri(), parameters(), headers(), of(service), payload(), ttl());
  }

  @Override
  public Request withHeader(String name, String value) {
    return withHeaders(ImmutableMap.of(name, value));
  }

  @Override
  public Request withHeaders(Map<String, String> additionalHeaders) {
    Map<String, String> headers = new LinkedHashMap<>(headers());
    headers.putAll(additionalHeaders);
    return create(method(), uri(), parameters(), headers, service(), payload(), ttl());
  }

  @Override
  public Request clearHeaders() {
    return create(method(), uri(), parameters(), emptyMap(), service(), payload(), ttl());
  }

  @Override
  public Request withPayload(ByteString payload) {
    return create(method(), uri(), parameters(), headers(), service(), of(payload), ttl());
  }

  @Override
  public Request withTtl(final long ttl) {
    return create(method(), uri(), parameters(), headers(), service(), payload(), of(ttl));
  }

  @Override
  public Request withTtl(final long ttl, final TimeUnit timeUnit) {
    return create(method(), uri(), parameters(), headers(), service(), payload(), of(timeUnit.toMillis(ttl)));
  }

  private static Map<String, List<String>> parseParameters(String uri) {
    return new QueryStringDecoder(uri).parameters();
  }
}
