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

import com.spotify.apollo.Client;
import com.spotify.apollo.Request;
import com.spotify.apollo.RequestContext;
import com.spotify.apollo.Response;
import com.spotify.apollo.dispatch.Endpoint;
import com.spotify.apollo.dispatch.EndpointInfo;
import com.spotify.apollo.request.EndpointRunnableFactory;
import com.spotify.apollo.request.OngoingRequest;
import com.spotify.apollo.request.RequestContexts;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okio.ByteString;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MetricsCollectingEndpointRunnableFactoryDecoratorTest {

  @Mock
  ServiceMetrics metrics;
  @Mock
  RequestMetrics requestStats;

  @Mock OngoingRequest ongoingRequest;
  @Mock Client client;
  @Mock Endpoint endpoint;
  @Mock EndpointInfo info;

  @Mock EndpointRunnableFactory delegate;
  @Mock Runnable delegateRunnable;

  @Captor ArgumentCaptor<OngoingRequest> ongoingRequestCaptor;
  @Captor ArgumentCaptor<RequestContext> requestContextCaptor;

  private RequestContext requestContext;
  private EndpointRunnableFactory endpointRunnableFactory;

  @Before
  public void setUp() throws Exception {
    Request request = Request.forUri("hm://foo");
    requestContext = RequestContexts.create(request, client, Collections.emptyMap());

    when(metrics.metricsForEndpointCall(any())).thenReturn(requestStats);

    when(ongoingRequest.request()).thenReturn(request);
    when(endpoint.info()).thenReturn(info);
    when(info.getName()).thenReturn("foo");

    when(delegate.create(ongoingRequestCaptor.capture(), requestContextCaptor.capture(), any()))
        .thenReturn(delegateRunnable);

    endpointRunnableFactory = new MetricsCollectingEndpointRunnableFactoryDecorator(metrics)
        .apply(delegate);
  }

  @Test
  public void shouldRunDelegate() throws Exception {
    endpointRunnableFactory.create(ongoingRequest, requestContext, endpoint).run();

    verify(delegateRunnable).run();
  }

  @Test
  public void shouldFinishOngoingRequest() throws Exception {
    endpointRunnableFactory.create(ongoingRequest, requestContext, endpoint).run();

    ongoingRequestCaptor.getValue()
        .reply(Response.ok());

    //noinspection unchecked
    verify(ongoingRequest).reply(any(Response.class));
  }

  @Test
  public void shouldTrackFanout() throws Exception {
    endpointRunnableFactory.create(ongoingRequest, requestContext, endpoint).run();

    requestContextCaptor.getValue()
        .requestScopedClient()
        .send(Request.forUri("http://example.com"));

    ongoingRequestCaptor.getValue()
        .reply(Response.ok());

    verify(requestStats).fanout(1);
  }

  @Test
  public void shouldCountRequest() throws Exception {
    endpointRunnableFactory.create(ongoingRequest, requestContext, endpoint).run();

    Response<ByteString> response = Response.ok();
    ongoingRequestCaptor.getValue()
        .reply(response);

    verify(requestStats).response(response);
  }

  @Test
  public void shouldForwardDrops() throws Exception {
    ExecutorService executorService = Executors.newSingleThreadExecutor();

    executorService
        .submit(endpointRunnableFactory.create(ongoingRequest, requestContext, endpoint))
        .get();

    ongoingRequestCaptor.getValue().drop();

    verify(requestStats).drop();
  }
}
