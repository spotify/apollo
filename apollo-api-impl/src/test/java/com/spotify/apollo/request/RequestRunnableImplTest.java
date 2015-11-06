package com.spotify.apollo.request;

import com.spotify.apollo.Request;
import com.spotify.apollo.Response;
import com.spotify.apollo.Status;
import com.spotify.apollo.dispatch.Endpoint;
import com.spotify.apollo.dispatch.EndpointInfo;
import com.spotify.apollo.route.ApplicationRouter;
import com.spotify.apollo.route.Rule;
import com.spotify.apollo.route.RuleMatch;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.Optional;
import java.util.function.BiConsumer;

import okio.ByteString;

import static com.spotify.apollo.Response.forStatus;
import static com.spotify.apollo.Status.INTERNAL_SERVER_ERROR;
import static com.spotify.apollo.Status.NOT_FOUND;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RequestRunnableImplTest {

  @Mock OngoingRequest ongoingRequest;
  @Mock ApplicationRouter<Endpoint> applicationRouter;
  @Mock RuleMatch<Endpoint> match;
  @Mock BiConsumer<OngoingRequest, RuleMatch<Endpoint>> matchContinuation;
  @Mock Endpoint endpoint;
  @Mock EndpointInfo info;
  @Mock Request message;

  @Captor ArgumentCaptor<Response<ByteString>> responseArgumentCaptor;

  RequestRunnableImpl requestRunnable;

  @Before
  public void setUp() throws Exception {
    when(applicationRouter.match(any(Request.class))).thenReturn(Optional.of(match));
    when(match.getRule()).thenReturn(Rule.fromUri("http://foo", "GET", endpoint));
    when(endpoint.info()).thenReturn(info);
    when(info.getName()).thenReturn("foo");
    when(ongoingRequest.request()).thenReturn(message);

    requestRunnable = new RequestRunnableImpl(ongoingRequest, applicationRouter);
  }

  @Test public void testRunsMatchedEndpoint() {
    requestRunnable.run(matchContinuation);

    verify(matchContinuation, times(1)).accept(eq(ongoingRequest), eq(match));
  }

  @Test
  public void testMatchingFails() throws Exception {
    when(applicationRouter.match(any(Request.class))).thenReturn(Optional.empty());
    when(applicationRouter.getMethodsForValidRules(any(Request.class)))
        .thenReturn(Collections.emptyList());

    requestRunnable.run(matchContinuation);

    verify(ongoingRequest).reply(forStatus(NOT_FOUND));
  }

  @Test
  public void testWrongMethod() throws Exception {
    when(applicationRouter.match(any(Request.class))).thenReturn(Optional.empty());
    when(applicationRouter.getMethodsForValidRules(any(Request.class)))
        .thenReturn(Collections.singleton("POST"));
    when(message.method()).thenReturn("GET");

    requestRunnable.run(matchContinuation);

    verify(ongoingRequest).reply(responseArgumentCaptor.capture());
    Response<ByteString> reply = responseArgumentCaptor.getValue();
    assertEquals(reply.statusCode(), Status.METHOD_NOT_ALLOWED);
    assertEquals(reply.headers(), Collections.singletonMap("Allow", "OPTIONS, POST"));
  }

  @Test
  public void testWithMethodOptions() throws Exception {
    when(applicationRouter.match(any(Request.class))).thenReturn(Optional.empty());
    when(applicationRouter.getMethodsForValidRules(any(Request.class)))
        .thenReturn(Collections.singleton("POST"));
    when(message.method()).thenReturn("OPTIONS");

    requestRunnable.run(matchContinuation);

    verify(ongoingRequest).reply(responseArgumentCaptor.capture());
    Response<ByteString> response = responseArgumentCaptor.getValue();
    assertThat(response.statusCode(), is(Status.NO_CONTENT));
    assertThat(response.headers(), is(Collections.singletonMap("Allow", "OPTIONS, POST")));
  }

  @Test
  public void shouldReply500IfApplicationRouterMatchThrows() throws Exception {
    when(applicationRouter.match(any(Request.class))).thenThrow(new RuntimeException("expected"));

    requestRunnable.run(matchContinuation);

    verify(ongoingRequest).reply(forStatus(INTERNAL_SERVER_ERROR));
  }

  @Test
  public void shouldReply500IfApplicationRouterValidMethodsThrows() throws Exception {
    when(applicationRouter.match(any(Request.class))).thenReturn(Optional.empty());
    when(applicationRouter.getMethodsForValidRules(any(Request.class)))
        .thenThrow(new RuntimeException("expected"));

    requestRunnable.run(matchContinuation);

    verify(ongoingRequest).reply(forStatus(INTERNAL_SERVER_ERROR));
  }
}
