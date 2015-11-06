package com.spotify.apollo.meta;

import com.spotify.apollo.Request;
import com.spotify.apollo.environment.IncomingRequestAwareClient;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static java.util.Optional.empty;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class OutgoingCallsGatheringClientTest {

  @Mock OutgoingCallsGatherer callsGatherer;
  @Mock IncomingRequestAwareClient delegate;

  IncomingRequestAwareClient client;

  @Before
  public void setUp() throws Exception {
    client = new OutgoingCallsGatheringClient(callsGatherer, delegate);
  }

  @Test
  public void shouldGatherCallsWithServiceFromAuthority() throws Exception {
    Request request = Request.forUri("http://bowman/path/to/file");
    client.send(request, empty());

    verify(delegate).send(eq(request), eq(empty()));
    verify(callsGatherer).gatherOutgoingCall(eq("bowman"), eq(request));
  }
}
