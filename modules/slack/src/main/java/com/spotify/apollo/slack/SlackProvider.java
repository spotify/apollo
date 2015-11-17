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

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spotify.apollo.core.Service;
import com.typesafe.config.Config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;

class SlackProvider implements Provider<Slack> {

  private static final Logger LOG = LoggerFactory.getLogger(SlackProvider.class);

  static final String CONFIG_PATH_WEBHOOK = "slack.webhook";
  static final String CONFIG_PATH_ENABLED = "slack.enabled";
  static final String CONFIG_PATH_USERNAME = "slack.username";
  static final String CONFIG_PATH_EMOJI = "slack.emoji";
  static final String CONFIG_PATH_MSG_STARTUP = "slack.messages.startup";
  static final String CONFIG_PATH_MSG_SHUTDOWN = "slack.messages.shutdown";

  private final String serviceName;
  private final Config config;

  @Inject
  public SlackProvider(Service service, Config config) {
    this.serviceName = service.getServiceName();
    this.config = config;
  }

  @Override
  public Slack get() {
    if (!SlackConfig.enabled(config)) {
      LOG.warn("Not loading Slack module");
      return new NoopSlackImpl();
    }

    SlackConfig slackConfig = SlackConfig.fromConfig(config, serviceName);
    return new SlackImpl(slackConfig);
  }

  static class SlackImpl implements Slack {

    private final Client client;
    private final WebTarget target;
    private final ObjectMapper mapper;
    private final SlackConfig slackConfig;

    public SlackImpl(SlackConfig slackConfig) {
      this.client = ClientBuilder.newBuilder().build();
      this.target = client.target(slackConfig.webhook());
      this.mapper = new ObjectMapper();
      this.slackConfig = slackConfig;

      if (!Strings.isNullOrEmpty(slackConfig.startupMsg())) {
        post(slackConfig.startupMsg());
      }
    }

    @Override
    public boolean post(String message) {
      Map<String, String> payload = ImmutableMap.of(
        "text", message,
        "username", slackConfig.username(),
        "icon_emoji", slackConfig.emoji()
      );

      try {
        Form form = new Form().param("payload", mapper.writeValueAsString(payload));
        this.target
            .request(MediaType.APPLICATION_JSON_TYPE)
            .post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));
        return true;
      } catch (IOException ex) {
        LOG.warn("Can't post message to slack", ex);
        return false;
      }
    }

    @Override
    public void close() throws IOException {
      if (!Strings.isNullOrEmpty(slackConfig.shutdownMsg())) {
        post(slackConfig.shutdownMsg());
      }
      client.close();
    }
  }

  static class NoopSlackImpl implements Slack {
    @Override
    public boolean post(String message) {
      LOG.warn("Didn't post to Slack because module is disabled");
      return false;
    }

    @Override
    public void close() throws IOException {
    }
  }

  static class SlackConfig {

    private final String webhook;
    private final String username;
    private final String emoji;
    private final String startupMsg;
    private final String shutdownMsg;

    public static boolean enabled(Config config) {
      return config.hasPath(CONFIG_PATH_WEBHOOK) &&
             getOrDefault(config, CONFIG_PATH_ENABLED, true);
    }

    public static SlackConfig fromConfig(Config config, String serviceName) {
      String webhook = config.getString(CONFIG_PATH_WEBHOOK);
      String username = getOrDefault(config, CONFIG_PATH_USERNAME, serviceName);
      String emoji = getOrDefault(config, CONFIG_PATH_EMOJI, ":spoticon:");
      String startupMsg = getOrDefault(config, CONFIG_PATH_MSG_STARTUP, "");
      String shutdwonMsg = getOrDefault(config, CONFIG_PATH_MSG_SHUTDOWN, "");
      return new SlackConfig(webhook, username, emoji, startupMsg, shutdwonMsg);
    }

    SlackConfig(String webhook, String username, String emoji, String startupMsg,
                String shutdownMsg) {
      this.webhook = webhook;
      this.username = username;
      this.emoji = emoji;
      this.startupMsg = startupMsg;
      this.shutdownMsg = shutdownMsg;
    }

    public String username() {
      return username;
    }

    public String emoji() {
      return emoji;
    }

    public String startupMsg() {
      return startupMsg;
    }

    public String shutdownMsg() {
      return shutdownMsg;
    }

    public String webhook() {
      return webhook;
    }

    private static boolean getOrDefault(Config config, String path, boolean defaultValue) {
      return config.hasPath(path) ? config.getBoolean(path) : defaultValue;
    }

    private static String getOrDefault(Config config, String path, String defaultValue) {
      return config.hasPath(path) ? config.getString(path) : defaultValue;
    }
  }

}
