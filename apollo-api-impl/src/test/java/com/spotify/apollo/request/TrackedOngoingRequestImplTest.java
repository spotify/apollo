package com.spotify.apollo.request;

import com.spotify.apollo.Request;
import com.spotify.apollo.Response;

import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class TrackedOngoingRequestImplTest {

  private RequestTracker tracker = new RequestTracker();

  @Before
  public void setUp() {

  }

  @Test
  public void shouldNotReplyIfNotTracked() throws Exception {
    Request requestMessage = Request.forUri("http://service/path");
    OngoingRequest ongoingRequest = mock(OngoingRequest.class);
    when(ongoingRequest.request()).thenReturn(requestMessage);
    when(ongoingRequest.isExpired()).thenReturn(false);

    final TrackedOngoingRequest trackedOngoingRequest =
        new TrackedOngoingRequestImpl(ongoingRequest, tracker);

    tracker.remove(trackedOngoingRequest);
    trackedOngoingRequest.reply(Response.ok());

    verifyNoMoreInteractions(ongoingRequest);
  }

}
