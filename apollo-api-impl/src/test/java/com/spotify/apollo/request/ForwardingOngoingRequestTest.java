/*
 * -\-\-
 * Spotify Apollo API Implementations
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
package com.spotify.apollo.request;

import com.spotify.apollo.Response;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.verify;

public class ForwardingOngoingRequestTest {
  private ForwardingOngoingRequest forwardingOngoingRequest;

  @Mock
  private OngoingRequest delegate;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    forwardingOngoingRequest = new ForwardingOngoingRequest(delegate) {
    };
  }

  @Test
  public void shouldForwardRequestMethod() throws Exception {
    forwardingOngoingRequest.request();

    verify(delegate).request();
  }

  @Test
  public void shouldForwardDrop() throws Exception {
    forwardingOngoingRequest.drop();

    verify(delegate).drop();
  }

  @Test
  public void shouldForwardReply() throws Exception {
    forwardingOngoingRequest.reply(Response.ok());

    verify(delegate).reply(Response.ok());
  }

  @Test
  public void shouldForwardMetadata() throws Exception {
    forwardingOngoingRequest.metadata();

    verify(delegate).metadata();
  }

  @Test
  public void shouldForwardArrivalTime() throws Exception {
    forwardingOngoingRequest.arrivalTimeNanos();

    verify(delegate).arrivalTimeNanos();
  }

  @Test
  public void shouldForwardIsExpired() throws Exception {
    forwardingOngoingRequest.isExpired();

    verify(delegate).isExpired();
  }

  @Test
  public void shouldForwardServerInfo() throws Exception {
    forwardingOngoingRequest.serverInfo();

    verify(delegate).serverInfo();
  }
}