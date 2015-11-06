package com.spotify.apollo.route;

import com.spotify.apollo.Response;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import okio.ByteString;

import static org.mockito.Mockito.mock;

public class VersionedRouteImplTest {

  VersionedRouteImpl versionedRoute;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Before
  public void setUp() throws Exception {
    //noinspection unchecked
    Route<AsyncHandler<Response<ByteString>>> route = mock(Route.class);

    // this might break later, if we change how versioned routes are instantiated
    versionedRoute = (VersionedRouteImpl) VersionedRoute.of(route);
  }

  @Test
  public void shouldDisallowNegativeValidFrom() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("non-negative");
    thrown.expectMessage("validFrom");

    versionedRoute.validFrom(-1);
  }

  @Test
  public void shouldDisallowNegativeRemovedIn() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("non-negative");
    thrown.expectMessage("removedIn");

    versionedRoute.removedIn(-10);
  }

  @Test
  public void shouldDisallowEmptyRange1() throws Exception {
    // store intermediate variable to ensure that the exception is thrown at the expected time
    VersionedRoute r = versionedRoute.validFrom(3);

    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("empty version range");
    thrown.expectMessage("[3, 1)");

    r.removedIn(1);
  }

  @Test
  public void shouldDisallowEmptyRange2() throws Exception {
    // store intermediate variable to ensure that the exception is thrown at the expected time
    VersionedRoute r = versionedRoute.validFrom(2);

    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("empty version range");
    thrown.expectMessage("[2, 2)");

    r.removedIn(2);
  }
}
