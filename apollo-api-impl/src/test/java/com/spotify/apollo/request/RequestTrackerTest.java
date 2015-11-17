package com.spotify.apollo.request;

import com.spotify.apollo.Request;
import com.spotify.apollo.Response;
import com.spotify.apollo.Status;
import com.spotify.apollo.StatusType;

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;

import okio.ByteString;

import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Matchers.argThat;
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

    verify(ongoingRequest).reply(argThat(hasStatus(Status.SERVICE_UNAVAILABLE)));
  }

  private Matcher<Response<ByteString>> hasStatus(StatusType status) {
    return new FeatureMatcher<Response<ByteString>, Integer>(
        is(status.code()), "status matches", "status") {

      @Override
      protected Integer featureValueOf(Response<ByteString> actual) {
        return actual.status().code();
      }
    };
  }
}
