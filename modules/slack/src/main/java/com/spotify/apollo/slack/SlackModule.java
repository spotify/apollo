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

import com.google.auto.service.AutoService;

import com.google.inject.Provider;
import com.spotify.apollo.module.AbstractApolloModule;
import com.spotify.apollo.module.ApolloModule;

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
