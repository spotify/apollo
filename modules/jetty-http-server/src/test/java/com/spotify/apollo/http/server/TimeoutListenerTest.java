/*
 * -\-\-
 * Spotify Apollo Jetty HTTP Server Module
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
package com.spotify.apollo.http.server;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TimeoutListenerTest {

  private TimeoutListener listener;

  private MockHttpServletResponse response;
  @Mock
  private AsyncContext asyncContext;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    response = new MockHttpServletResponse();
    listener = TimeoutListener.getInstance();

    when(asyncContext.getResponse()).thenReturn(response);
  }

  @Test
  public void shouldSendErrorResponseOnTimeout() throws Exception {
    listener.onTimeout(new AsyncEvent(asyncContext));

    assertThat(response.getStatus(), is(500));
    assertThat(response.getErrorMessage(), is("Timeout"));
  }

  @Test
  public void shouldCompleteContextOnTimeout() throws Exception {
    listener.onTimeout(new AsyncEvent(asyncContext));

    verify(asyncContext).complete();
  }
}