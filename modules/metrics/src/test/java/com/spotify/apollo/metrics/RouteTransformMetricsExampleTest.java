/*
 * -\-\-
 * Spotify Apollo Metrics Module
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
package com.spotify.apollo.metrics;

import com.codahale.metrics.Histogram;
import com.spotify.apollo.RequestContext;
import com.spotify.apollo.Response;
import com.spotify.apollo.route.AsyncHandler;
import com.spotify.apollo.route.Middleware;
import com.spotify.apollo.route.Route;
import com.spotify.metrics.core.MetricId;
import com.spotify.metrics.core.SemanticMetricRegistry;

import org.junit.Test;

import java.util.Collections;

import okio.ByteString;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasProperty;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RouteTransformMetricsExampleTest {
  private SemanticMetricRegistry registry;
  private String serviceName;

  public RouteTransformMetricsExampleTest() {
    registry = new SemanticMetricRegistry();
    serviceName = "example";
  }

  /**
   * Use this method to transform a route to one that tracks response payload sizes in a Histogram,
   * tagged with an endpoint tag set to method:uri of the route.
   */
  public Route<AsyncHandler<Response<ByteString>>> withResponsePayloadSizeHistogram(
      Route<AsyncHandler<Response<ByteString>>> route) {

    String endpointName = route.method() + ":" + route.uri();

    return route.withMiddleware(responsePayloadSizeHistogram(endpointName));
  }

  /**
   * Middleware to track response payload size in a Histogram,
   * tagged with an endpoint tag set to the given endpoint name.
   */
  public Middleware<AsyncHandler<Response<ByteString>>, AsyncHandler<Response<ByteString>>>
      responsePayloadSizeHistogram(String endpointName) {

    final MetricId histogramId = MetricId.build()
        .tagged("service", serviceName)
        .tagged("endpoint", endpointName)
        .tagged("what", "endpoint-response-size");

    final Histogram histogram = registry.histogram(histogramId);

    return (inner) -> (requestContext) ->
        inner.invoke(requestContext).whenComplete(
            (response, t) -> {
              if (response != null) {
                histogram.update(response.payload().map(ByteString::size).orElse(0));
              }
            }
        );
  }

  @Test
  public void shouldTrackResponsePayloadSize() throws Exception {
    Route<AsyncHandler<Response<ByteString>>> testRoute =
        Route.sync("GET", "/foo/<name>", context ->
            Response.forPayload(ByteString.encodeUtf8(context.pathArgs().get("name"))));

    Route<AsyncHandler<Response<ByteString>>> trackedRoute =
        withResponsePayloadSizeHistogram(testRoute);

    RequestContext context = mock(RequestContext.class);
    when(context.pathArgs()).thenReturn(Collections.singletonMap("name", "bar"));

    trackedRoute.handler().invoke(context).toCompletableFuture().get();

    assertThat(
        registry.getHistograms().keySet(),
        containsInAnyOrder(
            hasProperty("tags", allOf(
                hasEntry("service", "example"),
                hasEntry("what", "endpoint-response-size"),
                hasEntry("endpoint", "GET:/foo/<name>")
            ))));
  }
}
