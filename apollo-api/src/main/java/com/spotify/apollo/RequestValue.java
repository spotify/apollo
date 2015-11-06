/*
 * Copyright © 2015 Spotify AB
 */
package com.spotify.apollo;

import com.google.auto.value.AutoValue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    return create(method, uri, parseParameters(uri), emptyMap(), empty(), empty());
  }

  private static Request create(
      String method,
      String uri,
      Map<String, List<String>> parameters,
      Map<String, String> headers,
      Optional<String> service,
      Optional<ByteString> payload) {
    return new AutoValue_RequestValue(
        method, uri,
        parameters,
        headers,
        service,
        payload);
  }

  @Override
  public Request withService(String service) {
    return create(method(), uri(), parameters(), headers(), of(service), payload());
  }

  @Override
  public Request withHeader(String name, String value) {
    Map<String, String> headers = new LinkedHashMap<>(headers());
    headers.put(name, value);
    return create(method(), uri(), parameters(), headers, service(), payload());
  }

  @Override
  public Request withPayload(ByteString payload) {
    return create(method(), uri(), parameters(), headers(), service(), of(payload));
  }

  private static Map<String, List<String>> parseParameters(String uri) {
    return new QueryStringDecoder(uri).parameters();
  }
}
