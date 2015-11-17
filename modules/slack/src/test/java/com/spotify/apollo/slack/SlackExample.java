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

import com.spotify.apollo.core.Service;
import com.spotify.apollo.core.Services;

import org.junit.Ignore;

@Ignore
public class SlackExample {

  /**
   * Run this with your webhook as a JVM argument,
   * e.g. "-Dslack.webhook=https://hooks.slack.com/services/YOURPATH"
   */
  public static void main(String... args) throws Exception {
    Service service = Services.usingName("test")
        .usingModuleDiscovery(true)
        .withShutdownInterrupt(true)
        .build();

    try (Service.Instance instance = service.start(args)) {
      instance.waitForShutdown();
    }
  }

}
