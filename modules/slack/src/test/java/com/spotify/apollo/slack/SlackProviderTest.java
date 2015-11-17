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

import com.google.common.collect.ImmutableMap;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;

public class SlackProviderTest {

  @Test
  public void config() {
    Map<String, Object> config = ImmutableMap.<String,Object>of(
        SlackProvider.CONFIG_PATH_WEBHOOK, "http://example.com",
        SlackProvider.CONFIG_PATH_USERNAME, "username",
        SlackProvider.CONFIG_PATH_EMOJI, "emoji",
        SlackProvider.CONFIG_PATH_MSG_STARTUP, "startup",
        SlackProvider.CONFIG_PATH_MSG_SHUTDOWN, "shutdown"
    );
    SlackProvider.SlackConfig slackConfig =
        SlackProvider.SlackConfig.fromConfig(ConfigFactory.parseMap(config), "servicename");

    assertEquals(config.get(SlackProvider.CONFIG_PATH_USERNAME), slackConfig.username());
    assertEquals(config.get(SlackProvider.CONFIG_PATH_EMOJI), slackConfig.emoji());
    assertEquals(config.get(SlackProvider.CONFIG_PATH_MSG_STARTUP), slackConfig.startupMsg());
    assertEquals(config.get(SlackProvider.CONFIG_PATH_MSG_SHUTDOWN), slackConfig.shutdownMsg());
  }

  @Test
  public void configDefaultsToServicename() {
    final String servicename = "servicename";
    Map<String, Object> configMap = ImmutableMap.<String,Object>of(
        SlackProvider.CONFIG_PATH_WEBHOOK, "http://example.com"
    );
    final Config config = ConfigFactory.parseMap(configMap);
    SlackProvider.SlackConfig slackConfig =
        SlackProvider.SlackConfig.fromConfig(config, servicename);

    assertEquals(servicename, slackConfig.username());
  }

  @Test
  public void configDefaults() {
    Map<String, Object> configMap = ImmutableMap.<String,Object>of(
        SlackProvider.CONFIG_PATH_WEBHOOK, "http://example.com"
    );
    final Config config = ConfigFactory.parseMap(configMap);
    SlackProvider.SlackConfig slackConfig = SlackProvider.SlackConfig.fromConfig(config, "nop");

    assertEquals(true, SlackProvider.SlackConfig.enabled(config));
    assertEquals("nop", slackConfig.username());
    assertEquals(":spoticon:", slackConfig.emoji());
    assertEquals("", slackConfig.startupMsg());
    assertEquals("", slackConfig.shutdownMsg());
  }

}
