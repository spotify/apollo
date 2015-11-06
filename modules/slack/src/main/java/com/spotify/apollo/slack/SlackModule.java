package com.spotify.apollo.slack;

import com.google.auto.service.AutoService;

import com.spotify.apollo.module.AbstractApolloModule;
import com.spotify.apollo.module.ApolloModule;

import javax.inject.Provider;

@AutoService(ApolloModule.class)
public class SlackModule extends AbstractApolloModule {

  private final Class<? extends Provider<? extends Slack>> slackProvider;

  // Visible for SPI support
  public SlackModule() {
    slackProvider = SlackProvider.class;
  }

  SlackModule(Class<? extends Provider<? extends Slack>> slackProvider) {
    this.slackProvider = slackProvider;
  }

  public static SlackModule create() {
    return new SlackModule();
  }

  @Override
  protected void configure() {
    bind(Slack.class).toProvider(slackProvider);
    manageLifecycle(Slack.class);
  }

  @Override
  public String getId() {
    return "slack";
  }

  @Override
  public double getPriority() {
    // Very late in the priority chain
    return -1024.0;
  }
}
