package com.spotify.apollo.request;

import com.spotify.apollo.Request;
import com.spotify.apollo.Response;
import com.spotify.apollo.StatusType;

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;

import okio.ByteString;

import static com.spotify.apollo.Status.INTERNAL_SERVER_ERROR;
import static com.spotify.apollo.request.TrackedOngoingRequest.FailureCause.TIMEOUT;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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

  @Test
  public void shouldReplyWhenFailed() throws Exception {
    Request requestMessage = Request.forUri("http://service/path");
    OngoingRequest ongoingRequest = mock(OngoingRequest.class);
    when(ongoingRequest.request()).thenReturn(requestMessage);
    when(ongoingRequest.isExpired()).thenReturn(false);

    new TrackedOngoingRequestImpl(ongoingRequest, tracker)
        .fail(TIMEOUT);

    verify(ongoingRequest).reply(argThat(hasStatus(INTERNAL_SERVER_ERROR)));
  }

  @Test
  public void shouldNotReplyWhenFailedIfRemoved() throws Exception {
    Request requestMessage = Request.forUri("http://service/path");
    OngoingRequest ongoingRequest = mock(OngoingRequest.class);
    when(ongoingRequest.request()).thenReturn(requestMessage);
    when(ongoingRequest.isExpired()).thenReturn(false);

    final TrackedOngoingRequest trackedOngoingRequest =
        new TrackedOngoingRequestImpl(ongoingRequest, tracker);

    tracker.remove(trackedOngoingRequest);
    trackedOngoingRequest.fail(TIMEOUT);

    verifyNoMoreInteractions(ongoingRequest);
  }

  private Matcher<Response<ByteString>> hasStatus(StatusType status) {
    return new FeatureMatcher<Response<ByteString>, Integer>(
        is(status.statusCode()), "status matches", "status") {

      @Override
      protected Integer featureValueOf(Response<ByteString> actual) {
        return actual.statusCode().statusCode();
      }
    };
  }
}
