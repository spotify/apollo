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
import com.google.common.base.CharMatcher;

import java.text.MessageFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Lists more-or-less common status codes, taken from
 * http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html.
 *
 * More status codes can be defined by implementing the {@link StatusType} interface, for
 * instance through the {@link #createForCode(int)} method. Reason phrases can be overridden
 * (returning a different object than the original enum constant) using the
 * {@link #withReasonPhrase(String)} method.
 */
public enum Status implements StatusType {
  OK(200, "OK"),
  CREATED(201, "Created"),
  ACCEPTED(202, "Accepted"),
  NO_CONTENT(204, "No Content"),
  RESET_CONTENT(205, "Reset Content"),
  PARTIAL_CONTENT(206, "Partial Content"),
  MOVED_PERMANENTLY(301, "Moved Permanently"),
  FOUND(302, "Found"),
  SEE_OTHER(303, "See Other"),
  NOT_MODIFIED(304, "Not Modified"),
  USE_PROXY(305, "Use Proxy"),
  TEMPORARY_REDIRECT(307, "Temporary Redirect"),
  BAD_REQUEST(400, "Bad Request"),
  UNAUTHORIZED(401, "Unauthorized"),
  PAYMENT_REQUIRED(402, "Payment Required"),
  FORBIDDEN(403, "Forbidden"),
  NOT_FOUND(404, "Not Found"),
  METHOD_NOT_ALLOWED(405, "Method Not Allowed"),
  NOT_ACCEPTABLE(406, "Not Acceptable"),
  PROXY_AUTHENTICATION_REQUIRED(407, "Proxy Authentication Required"),
  REQUEST_TIMEOUT(408, "Request Timeout"),
  CONFLICT(409, "Conflict"),
  GONE(410, "Gone"),
  LENGTH_REQUIRED(411, "Length Required"),
  PRECONDITION_FAILED(412, "Precondition Failed"),
  REQUEST_ENTITY_TOO_LARGE(413, "Request Entity Too Large"),
  REQUEST_URI_TOO_LONG(414, "Request-URI Too Long"),
  UNSUPPORTED_MEDIA_TYPE(415, "Unsupported Media Type"),
  REQUESTED_RANGE_NOT_SATISFIABLE(416, "Requested Range Not Satisfiable"),
  EXPECTATION_FAILED(417, "Expectation Failed"),
  IM_A_TEAPOT(418, "I'm a Teapot"),
  UNPROCESSABLE_ENTITY(422, "Unprocessable Entity"),
  TOO_MANY_REQUESTS(429, "Too Many Requests"),
  INTERNAL_SERVER_ERROR(500, "Internal Server Error"),
  NOT_IMPLEMENTED(501, "Not Implemented"),
  BAD_GATEWAY(502, "Bad Gateway"),
  SERVICE_UNAVAILABLE(503, "Service Unavailable"),
  GATEWAY_TIMEOUT(504, "Gateway Timeout");

  // see http://tools.ietf.org/html/rfc2616#section-6.1.1 - any TEXT except CRLF, and
  // TEXT is any octet except 0-31 and 127.
  private static final CharMatcher ILLEGAL_REASONPHRASE_CHARS =
      CharMatcher.anyOf("\n\r")
          .or(CharMatcher.inRange((char) 0, (char) 31))
          .or(CharMatcher.is((char) 127));
  private static final Map<Integer, Status> EXISTING = new LinkedHashMap<>();
  private final int statusCode;
  private final String reasonPhrase;

  static {
    for (Status status : values()) {
      EXISTING.put(status.statusCode, status);
    }
  }

  Status(int statusCode, String reasonPhrase) {
    this.statusCode = statusCode;
    this.reasonPhrase = reasonPhrase;
  }

  @Override
  public int code() {
    return statusCode;
  }

  @Override
  public String reasonPhrase() {
    return reasonPhrase;
  }

  @Override
  public Family family() {
    return Family.familyOf(statusCode);
  }

  @Override
  public StatusType withReasonPhrase(String reasonPhrase) {
    return WithReasonPhrase.create(code(), reasonPhrase);
  }

  public static Status existingFromStatusCode(int statusCode) {
    return findExistingFromStatusCode(statusCode)
        .orElseThrow(() -> new IllegalArgumentException(
            MessageFormat.format("No status with status code {0} found", statusCode)));
  }

  public static Optional<Status> findExistingFromStatusCode(int statusCode) {
    return Optional.ofNullable(EXISTING.get(statusCode));
  }

  public static StatusType createForCode(int statusCode) {
    Status existingStatus = EXISTING.get(statusCode);
    if (existingStatus != null) {
      return existingStatus;
    } else {
      return WithReasonPhrase.create(statusCode, "");
    }
  }

  @AutoValue
  public static abstract class WithReasonPhrase implements StatusType {
    @Override
    public abstract int code();

    @Override
    public abstract String reasonPhrase();

    @Override
    public abstract Family family();

    public static WithReasonPhrase create(int statusCode, String reasonPhrase) {
      String safeReasonPhrase;
      if (reasonPhrase == null) {
        safeReasonPhrase = "";
      } else {
        safeReasonPhrase = ILLEGAL_REASONPHRASE_CHARS.replaceFrom(reasonPhrase, ' ');
      }
      return new AutoValue_Status_WithReasonPhrase(statusCode, safeReasonPhrase, Family.familyOf(statusCode));
    }

    @Override
    public StatusType withReasonPhrase(String reasonPhrase) {
      return create(code(), reasonPhrase);
    }
  }
}
