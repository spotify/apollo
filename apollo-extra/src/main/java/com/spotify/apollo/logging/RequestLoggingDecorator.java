/*
 * -\-\-
 * Spotify Apollo Extra
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
package com.spotify.apollo.logging;

import com.google.inject.Inject;

import com.spotify.apollo.environment.EndpointRunnableFactoryDecorator;
import com.spotify.apollo.request.EndpointRunnableFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.TextStyle;
import java.time.temporal.ChronoField;
import java.util.Locale;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * Logs requests and their outcomes. The method for logging is configurable via the
 * {@link #setLogger(RequestOutcomeConsumer)} method.
 * <p/>
 * By default, uses a log format based on an approximation of the
 * combined log format from Apache HTTPD (http://httpd.apache.org/docs/1.3/logs.html#combined).
 * Known divergences:
 *  - not reporting the protocol version in the request line, because this information isn't
 *    surfaced by underlying Apollo layers
 *  - never reporting the remote address, because this information isn't surfaced by underlying
 *    Apollo layers
 *  - if a request was dropped, a dash ('-') is logged instead of a numeric response code
 *  - remote ident is not supported (always '-')
 *  - remote user is not supported (always '-')
 */
public class RequestLoggingDecorator implements EndpointRunnableFactoryDecorator {

  private static final Logger LOGGER = LoggerFactory.getLogger(RequestLoggingDecorator.class);
  private static final DateTimeFormatter DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
      .appendLiteral("[")
      .appendValue(ChronoField.DAY_OF_MONTH, 2)
      .appendLiteral('/')
      .appendText(ChronoField.MONTH_OF_YEAR, TextStyle.SHORT)
      .appendLiteral('/')
      .appendValue(ChronoField.YEAR, 4)
      .appendLiteral(':')
      .appendValue(ChronoField.HOUR_OF_DAY, 2)
      .appendLiteral(':')
      .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
      .appendLiteral(':')
      .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
      .appendLiteral(' ')
      .appendOffset("+HHMM", "UTC")
      .appendLiteral(']')
      .toFormatter(Locale.ENGLISH);
  private static final RequestOutcomeConsumer
      LOG_WITH_COMBINED_FORMAT =
      (ongoingRequest, response) ->
          LOGGER.info("- - - {} \"{}\" {} {} \"{}\" \"{}\"",
                      DATE_TIME_FORMATTER.format(ZonedDateTime.now()),
                      String.format("%s %s", ongoingRequest.request().method(),
                                    ongoingRequest.request().uri()),
                      response.map(r -> String.valueOf(r.status().code())).orElse("DROPPED"),
                      response.flatMap(
                          r -> r.payload().flatMap(p -> Optional.of(String.valueOf(p.size()))))
                          .orElse("-"),
                      ongoingRequest.request().header("Referer").orElse("-"),
                      ongoingRequest.request().header("User-Agent").orElse("-"));

  private RequestOutcomeConsumer logger = LOG_WITH_COMBINED_FORMAT;

  @Inject(optional = true)
  public void setLogger(RequestOutcomeConsumer logger) {
    this.logger = requireNonNull(logger);
  }

  @Override
  public EndpointRunnableFactory apply(EndpointRunnableFactory delegate) {
    return ((ongoingRequest, requestContext, endpoint) ->
                delegate.create(new OutcomeReportingOngoingRequest(ongoingRequest, logger),
                                requestContext,
                                endpoint));
  }
}
