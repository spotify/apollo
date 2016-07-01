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

import com.spotify.apollo.Response;
import com.spotify.apollo.request.OngoingRequest;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import okio.ByteString;

import static com.spotify.apollo.Status.BAD_REQUEST;
import static org.mockito.Mockito.verify;

public class MetricsTrackingOngoingRequestTest {
  private MetricsTrackingOngoingRequest request;

  @Mock
  RequestMetrics requestStats;
  @Mock
  OngoingRequest delegate;
  private Response<ByteString> response;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    request = new MetricsTrackingOngoingRequest(requestStats, delegate);
    response = Response.forStatus(
        BAD_REQUEST.withReasonPhrase("making it a little unusual for better tests"));
  }

  @Test
  public void shouldAccountForResponses() throws Exception {
    request.reply(response);

    verify(requestStats).response(response);
  }

  @Test
  public void shouldAccountForDrops() throws Exception {
    request.drop();

    verify(requestStats).drop();
  }

  @Test
  public void shouldAccountForDownstreamRequestCountOnResponse() throws Exception {
    request.incrementDownstreamRequests();
    request.incrementDownstreamRequests();
    request.incrementDownstreamRequests();
    request.incrementDownstreamRequests();

    request.reply(response);

    verify(requestStats).fanout(4);
  }

  @Test
  public void shouldForwardRepliesToDelegate() throws Exception {
    request.reply(response);

    verify(delegate).reply(response);
  }

  @Test
  public void shouldForwardDropsToDelegate() throws Exception {
    request.drop();

    verify(delegate).drop();
  }
}
