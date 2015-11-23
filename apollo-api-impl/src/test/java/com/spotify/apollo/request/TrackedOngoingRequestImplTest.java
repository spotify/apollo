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
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TrackedOngoingRequestImplTest {

  @Mock OngoingRequest ongoingRequest;
  @Mock RequestTracker requestTracker;

  RequestTracker tracker = new RequestTracker();

  @Before
  public void setUp() throws Exception {
    when(ongoingRequest.request()).thenReturn(Request.forUri("http://service/path"));
    when(ongoingRequest.isExpired()).thenReturn(false);
  }

  @Test
  public void shouldRegisterWithRequestTracker() throws Exception {
    OngoingRequest tr = new TrackedOngoingRequestImpl(ongoingRequest, requestTracker);

    verify(requestTracker).register(eq(tr));
  }

  @Test
  public void shouldNotReplyIfNotTracked() throws Exception {
    final OngoingRequest trackedOngoingRequest =
        new TrackedOngoingRequestImpl(ongoingRequest, tracker);

    tracker.remove(trackedOngoingRequest);
    trackedOngoingRequest.reply(Response.ok());

    verifyNoMoreInteractions(ongoingRequest);
  }
}
