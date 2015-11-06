package com.spotify.apollo.slack;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SlackModuleTest {

  @Test
  public void loadWithWebhook() {
    final Config config = ConfigFactory.parseString(
        "slack.webhook: \"https://hooks.slack.com/services/smth\"");
    assertTrue(SlackProvider.SlackConfig.enabled(config));
  }

  @Test
  public void dontLoadWithoutWebhook() {
    assertFalse(SlackProvider.SlackConfig.enabled(ConfigFactory.empty()));
  }

}
