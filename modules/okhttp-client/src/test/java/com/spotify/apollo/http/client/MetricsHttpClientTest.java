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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isA;
import static org.mockito.Mockito.when;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.spotify.apollo.Request;
import com.spotify.apollo.Response;
import com.spotify.apollo.Status;
import com.spotify.apollo.environment.IncomingRequestAwareClient;
import com.spotify.metrics.core.MetricId;
import com.spotify.metrics.core.SemanticMetricRegistry;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import okio.ByteString;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Strict.class)
public class MetricsHttpClientTest {

  @Mock private IncomingRequestAwareClient client;

  private SemanticMetricRegistry semanticMetricRegistry = new SemanticMetricRegistry();

  @InjectMocks private MetricsHttpClient sut;

  private Request standardRequest = Request.forUri("http://www.spotify.com");

  @Rule public ExpectedException thrown = ExpectedException.none();

  @Before
  public void setUp() {
    sut = new MetricsHttpClient(client, semanticMetricRegistry);
  }

  @Test
  public void testNonHttpRequestSentAsStandard() throws ExecutionException, InterruptedException {

    final Request request = Request.forUri("ftp://a-thing.com");
    when(client.send(request, Optional.empty()))
        .thenReturn(CompletableFuture.completedFuture(Response.ok()));
    final Response<ByteString> response =
        sut.send(request, Optional.empty()).toCompletableFuture().get();
    assertThat(response.status(), is(Status.OK));
  }

  @Test
  public void testHttpRequestReturnsCompletionStage()
      throws ExecutionException, InterruptedException {
    when(client.send(standardRequest, Optional.empty()))
        .thenReturn(CompletableFuture.completedFuture(Response.ok()));
    final Response<ByteString> response =
        sut.send(standardRequest, Optional.empty()).toCompletableFuture().get();
    assertThat(response.status(), is(Status.OK));
  }

  @Test
  public void testHttpRequestExceptionPath() throws ExecutionException, InterruptedException {
    when(client.send(standardRequest, Optional.empty()))
        .thenReturn(
            CompletableFuture.supplyAsync(
                () -> {
                  throw new RuntimeException("failed");
                }));

    thrown.expectCause(isA(RuntimeException.class));
    thrown.expectMessage("failed");

    sut.send(standardRequest, Optional.empty()).toCompletableFuture().get();
  }
}
