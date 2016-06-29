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

import static org.mockito.Mockito.verify;

public class MetricsTrackingOngoingRequestTest {
  MetricsTrackingOngoingRequest request;


  @Mock
  ApolloRequestMetrics requestStats;
  @Mock
  OngoingRequest delegate;
  @Mock
  ApolloTimerContext timerContext;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    request = new MetricsTrackingOngoingRequest(requestStats, delegate, timerContext);
  }

  @Test
  public void shouldStopTimerOnReply() throws Exception {
    request.reply(Response.ok());

    verify(timerContext).stop();
  }

  @Test
  public void shouldStopTimerOnDrop() throws Exception {
    request.drop();

    verify(timerContext).stop();
  }
}
