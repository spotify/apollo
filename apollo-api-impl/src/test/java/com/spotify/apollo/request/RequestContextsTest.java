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
