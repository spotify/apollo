package com.spotify.apollo.request;

import com.spotify.apollo.Request;

import org.junit.Before;
import org.junit.Test;

import static com.spotify.apollo.request.TrackedOngoingRequest.FailureCause.TIMEOUT;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RequestTrackerTest {

  private RequestTracker tracker;

  @Before
  public void setUp() {
    tracker = new RequestTracker();
  }

  @Test
  public void droppedAfterExpiration() {
    Request requestMessage = Request.forUri("http://service/path");
    OngoingRequest ongoingRequest = mock(OngoingRequest.class);
    when(ongoingRequest.request()).thenReturn(requestMessage);
    when(ongoingRequest.isExpired()).thenReturn(true);

    new TrackedOngoingRequestImpl(ongoingRequest, tracker);
    tracker.reap();

    verify(ongoingRequest).drop();
  }

  @Test
  public void shouldFailRequestsWhenClosed() throws Exception {
    Request requestMessage = Request.forUri("http://service/path");
    TrackedOngoingRequest ongoingRequest = mock(TrackedOngoingRequest.class);
    when(ongoingRequest.request()).thenReturn(requestMessage);
    when(ongoingRequest.isExpired()).thenReturn(false);

    tracker.register(ongoingRequest);
    tracker.close();

    verify(ongoingRequest).fail(TIMEOUT);
  }
}
