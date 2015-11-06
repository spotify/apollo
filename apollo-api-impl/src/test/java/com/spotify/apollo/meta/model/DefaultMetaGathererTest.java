/*
 * Copyright © 2014 Spotify AB
 */
package com.spotify.apollo.meta.model;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertThat;

public class DefaultMetaGathererTest {

  private static final String buildVersion = "1.2.3.4";
  private static final String containerVersion = "freight-container 3.2.1";

  MetaGatherer gatherer;

  @Before
  public void setUp() throws Exception {
    setUpNoConfig();
  }

  private void setUpNoConfig() {
    Model.MetaInfo metaInfo = new MetaInfoBuilder()
        .buildVersion(buildVersion)
        .containerVersion(containerVersion)
        .build();

    gatherer = Meta.createGatherer(metaInfo);
  }

  @Test
  public void testInfo() throws InterruptedException {
    Thread.sleep(10);
    Model.MetaInfo metaInfo = gatherer.info();

    assertThat(metaInfo.buildVersion(), is(buildVersion));
    assertThat(metaInfo.containerVersion(), is(containerVersion));
    assertThat(metaInfo.serviceUptime(), is(greaterThan(0.0)));
    assertThat(metaInfo.systemVersion(), is(notNullValue()));
  }
}
