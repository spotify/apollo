package com.spotify.apollo;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nullable;

/**
 * An immutable response. Each invocation of a 'withXXX' method returns a new instance,
 * making it safe to share instances between threads, at the cost of some efficiency.
 */
@AutoValue
abstract class ResponseImpl<T> implements Response<T> {

  static final Response<?> OK = createInternal(Status.OK, Optional.empty());

  @Override
  public Response<T> withHeader(String header, String value) {
    // Allow overriding values
    final Map<String, String> headers = new LinkedHashMap<>();
    headers.putAll(headers());
    headers.put(header, value);

    return createInternal(statusCode(), ImmutableMap.copyOf(headers), payload());
  }

  @Override
  public <P> Response<P> withPayload(@Nullable P newPayload) {
    return createInternal(statusCode(), headers(), Optional.ofNullable(newPayload));
  }

  static <T> Response<T> create(StatusType statusCode) {
    Objects.requireNonNull(statusCode);

    //noinspection unchecked
    return statusCode == Status.OK
        ? (Response<T>) OK
        : createInternal(statusCode, Optional.<T>empty());
  }

  static <T> Response<T> create(StatusType statusCode, T payload) {
    Objects.requireNonNull(statusCode);
    return createInternal(statusCode, Optional.of(payload));
  }

  static <T> Response<T> create(T payload) {
    return createInternal(Status.OK, Optional.of(payload));
  }

  private static <T> Response<T> createInternal(StatusType statusCode, Optional<T> payload) {
    return createInternal(
        statusCode,
        Collections.emptyMap(),
        payload);
  }

  private static <T> Response<T> createInternal(
      StatusType statusCode,
      Map<String, String> headers,
      Optional<T> payload) {
    return new AutoValue_ResponseImpl<>(
        statusCode,
        headers,
        payload);
  }
}
