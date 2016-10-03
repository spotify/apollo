/*
 * -\-\-
 * Spotify Apollo API Implementations
 * --
 * Copyright (C) 2013 - 2015 Spotify AB
 * --
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -/-/-
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
        .componentId("freight-container")
        .buildVersion(buildVersion)
        .containerVersion(containerVersion)
        .build();

    gatherer = Meta.createGatherer(metaInfo);
  }

  @Test
  public void testInfo() throws InterruptedException {
    Thread.sleep(10);
    Model.MetaInfo metaInfo = gatherer.info();

    assertThat(metaInfo.componentId(), is("freight-container"));
    assertThat(metaInfo.buildVersion(), is(buildVersion));
    assertThat(metaInfo.containerVersion(), is(containerVersion));
    assertThat(metaInfo.serviceUptime(), is(greaterThan(0.0)));
    assertThat(metaInfo.systemVersion(), is(notNullValue()));
  }
}
