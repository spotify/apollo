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
