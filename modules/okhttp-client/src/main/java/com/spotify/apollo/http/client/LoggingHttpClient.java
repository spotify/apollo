/*-
 * -\-\-
 * Spotify Apollo okhttp Client Module
 * --
 * Copyright (C) 2013 - 2021 Spotify AB
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

package com.spotify.apollo.http.client;

import com.spotify.apollo.Request;
import com.spotify.apollo.Response;
import com.spotify.apollo.environment.IncomingRequestAwareClient;
import com.spotify.metrics.core.MetricId;
import com.spotify.metrics.core.SemanticMetricRegistry;
import java.net.URI;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nullable;
import okio.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingHttpClient implements IncomingRequestAwareClient {

  private static final Logger logger = LoggerFactory.getLogger(LoggingHttpClient.class);

  private final IncomingRequestAwareClient client;
  private final SemanticMetricRegistry metricRegistry;
  private final MetricId baseId = MetricId.build();

  public LoggingHttpClient(
      IncomingRequestAwareClient client, SemanticMetricRegistry metricRegistry) {
    this.client = client;
    this.metricRegistry = metricRegistry;
  }

  @Override
  public CompletionStage<Response<ByteString>> send(Request request, Optional<Request> incoming) {

    if (!request.uri().startsWith("http")) {
      return client.send(request, incoming);
    }

    final long start = System.currentTimeMillis();

    return client
        .send(request, incoming)
        .whenComplete(
            (response, throwable) -> {
              final long elapsedMillis = System.currentTimeMillis() - start;
              if (throwable != null) {
                logOutgoingRequest(request, elapsedMillis, null);
                logger.error("Exception caught: ", throwable);
              } else {
                logOutgoingRequest(request, elapsedMillis, response);
              }
            });
  }

  private void logOutgoingRequest(
      final Request request, final long elapsedMillis, @Nullable final Response<?> response) {
    final String statusCode =
        response != null ? Integer.toString(response.status().code()) : "EXCEPTION";

    final String targetHost = getHost(request);

    metricRegistry
        .meter(
            baseId
                .tagged("what", "message-rate")
                .tagged("status-code", statusCode)
                .tagged("target-host", targetHost)
                .tagged("protocol", "http/https")
                .tagged("unit", "request"))
        .mark();

    metricRegistry
        .histogram(
            baseId
                .tagged("what", "request-latency")
                .tagged("unit", "ms")
                .tagged("protocol", "http/https")
                .tagged("target-host", targetHost))
        .update(elapsedMillis);
  }

  private String getHost(Request request) {
    try {
      return URI.create(request.uri()).getHost();
    } catch (IllegalArgumentException e) {
      return "UNKNOWN";
    }
  }
}
