package com.spotify.apollo.route;

import com.spotify.apollo.RequestContext;

import org.junit.Before;
import org.junit.Test;

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
}
