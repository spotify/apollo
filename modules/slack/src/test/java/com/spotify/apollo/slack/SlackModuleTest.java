/*
 * -\-\-
 * Spotify Apollo Slack Module
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
package com.spotify.apollo.slack;


import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SlackModuleTest {

  @Test
  public void loadWithWebhook() {
    fail();
//    final Config config = ConfigFactory.parseString(
//        "slack.webhook: \"https://hooks.slack.com/services/smth\"");
//    assertTrue(SlackProvider.SlackConfig.enabled(config));
  }

  @Test
  public void dontLoadWithoutWebhook() {
    fail();
//    assertFalse(SlackProvider.SlackConfig.enabled(ConfigFactory.empty()));
  }

}
