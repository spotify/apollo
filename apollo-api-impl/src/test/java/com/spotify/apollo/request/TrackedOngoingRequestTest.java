package com.spotify.apollo.request;

import com.spotify.apollo.Request;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TrackedOngoingRequestTest {

  @Mock OngoingRequest ongoingRequest;
  @Mock RequestTracker requestTracker;

  @Before
  public void setUp() throws Exception {
    when(ongoingRequest.request())
        .thenReturn(Request.forUri("http://foo"));
  }

  @Test
  public void shouldRegisterWithRequestTracker() throws Exception {
    TrackedOngoingRequest tr = new TrackedOngoingRequestImpl(ongoingRequest, requestTracker);

    verify(requestTracker).register(eq(tr));
  }
}
