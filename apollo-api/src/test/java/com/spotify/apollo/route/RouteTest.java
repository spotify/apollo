/*
 * -\-\-
 * Spotify Apollo API Interfaces
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
package com.spotify.apollo.route;

import com.spotify.apollo.RequestContext;
import com.spotify.apollo.route.Route.DocString;

import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class RouteTest {

  RequestContext requestContext;

  @Before
  public void setUp() throws Exception {
    requestContext = mock(RequestContext.class);
  }

  @Test
  public void shouldSupportSyncEndpointHandler() throws Exception {
    Route<AsyncHandler<String>> route =
        Route.sync("GET", "/foo", requestContext -> "this is the best response");

    String actual = route.handler().invoke(requestContext).toCompletableFuture().get();
    assertThat(actual, equalTo("this is the best response"));
  }

  @Test
  public void shouldSupportSupplyingDocStringValueType() throws Exception {
    Route<AsyncHandler<String>> route =
        Route.sync("GET", "/foo", requestContext -> "this is the best response")
            .withDocString(DocString.doc("summary", "description"));

    assertThat(route.docString(), equalTo(Optional.of(DocString.doc("summary", "description"))));
  }
}
