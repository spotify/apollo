package com.spotify.apollo.meta;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class ApplicationOrMetaRouterTest {
  @Test
  public void shouldMatchMeta() {
    String uri = "gopher://au.th/_meta/0/info";
    assertThat(ApplicationOrMetaRouter.isMeta(uri), is(true));
  }
}
