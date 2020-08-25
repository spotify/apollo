/*-
 * -\-\-
 * Spotify Apollo API Implementations
 * --
 * Copyright (C) 2013 - 2020 Spotify AB
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

import static org.junit.Assert.assertEquals;

import com.spotify.apollo.Client;
import com.spotify.apollo.Request;
import com.spotify.apollo.RequestContext;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RequestContextsTest {

  @Mock Client client;

  @Test
  public void shouldReturnEmptyCallerIdentity() {
    RequestContext requestContext =
        RequestContexts.create(
            Request.forUri("http://foo"),
            client,
            Collections.emptyMap(),
            System.nanoTime(),
            RequestMetadataImpl.create(Instant.now(), Optional.empty(), Optional.empty()));
    assertEquals(Optional.empty(), requestContext.metadata().callerIdentity());
  }
}
