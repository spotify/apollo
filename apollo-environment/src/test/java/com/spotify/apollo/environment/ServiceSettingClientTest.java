/*
 * -\-\-
 * Spotify Apollo API Environment
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
package com.spotify.apollo.environment;

import com.spotify.apollo.Request;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ServiceSettingClientTest {

  private static final String TEST_SERVICE = "test-service";

  @Mock IncomingRequestAwareClient delegate;
  @Captor ArgumentCaptor<Request> sentRequest;

  ServiceSettingClient client;

  @Before
  public void setup() {
    when(delegate.send(sentRequest.capture(), any())).thenReturn(null);
    client = new ServiceSettingClient(TEST_SERVICE, delegate);
  }

  @Test
  public void decoratorShouldAddService() throws Exception {
    Request outgoing = Request.forUri("http://downstream");
    client.send(outgoing, Optional.empty());

    Request sent = sentRequest.getValue();

    assertEquals(TEST_SERVICE, sent.service().get());
  }

  @Test
  public void decoratorShouldNotOverrideService() throws Exception {
    Request outgoing = Request.forUri("http://downstream").withService("manual");
    client.send(outgoing, Optional.empty());

    Request sent = sentRequest.getValue();

    assertEquals("manual", sent.service().get());
  }
}
