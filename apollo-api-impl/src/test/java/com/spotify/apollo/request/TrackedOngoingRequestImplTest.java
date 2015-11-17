/*
 * -\-\-
 * Spotify Apollo API Implementations
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
package com.spotify.apollo.request;

import com.spotify.apollo.Request;
import com.spotify.apollo.Response;

import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class TrackedOngoingRequestImplTest {

  private RequestTracker tracker = new RequestTracker();

  @Before
  public void setUp() {

  }

  @Test
  public void shouldNotReplyIfNotTracked() throws Exception {
    Request requestMessage = Request.forUri("http://service/path");
    OngoingRequest ongoingRequest = mock(OngoingRequest.class);
    when(ongoingRequest.request()).thenReturn(requestMessage);
    when(ongoingRequest.isExpired()).thenReturn(false);

    final TrackedOngoingRequest trackedOngoingRequest =
        new TrackedOngoingRequestImpl(ongoingRequest, tracker);

    tracker.remove(trackedOngoingRequest);
    trackedOngoingRequest.reply(Response.ok());

    verifyNoMoreInteractions(ongoingRequest);
  }

}
