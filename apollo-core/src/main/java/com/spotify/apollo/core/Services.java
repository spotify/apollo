/*
 * -\-\-
 * Spotify Apollo Service Core (aka Leto)
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
package com.spotify.apollo.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public final class Services {

  private static final Logger LOG = LoggerFactory.getLogger(Services.class);

  private Services() {
    throw new IllegalAccessError("This class may not be instantiated.");
  }

  public static Service.Builder usingName(String serviceName) {
    return ServiceImpl.builder(serviceName);
  }

  public static void run(Service service, String... args) throws IOException, InterruptedException {
    try (Service.Instance instance = service.start(args)) {
      LOG.info("Started service '{}'", service.getServiceName());
      instance.waitForShutdown();
      LOG.info("Stopping service '{}'", service.getServiceName());
    }
  }

  public static final String INJECT_SERVICE_NAME = "service-name";
  public static final String INJECT_UNPROCESSED_ARGS = "unprocessed-args";

  public enum CommonConfigKeys {
    LOGGING("logging"),
    LOGGING_VERBOSITY("logging.verbosity"),
    LOGGING_SYSLOG("logging.syslog"),
    LOGGING_CONFIG("logging.config"),
    APOLLO("apollo"),
    APOLLO_COMMAND("apollo.command"),
    APOLLO_BACKEND("apollo.backend"),
    APOLLO_DOMAIN("apollo.domain"),
    APOLLO_ARGS_CORE("apollo.args.core"),
    APOLLO_ARGS_UNPARSED("apollo.args.unparsed");

    private final String key;

    CommonConfigKeys(String key) {
      this.key = key;
    }

    public String getKey() {
      return key;
    }
  }
}
