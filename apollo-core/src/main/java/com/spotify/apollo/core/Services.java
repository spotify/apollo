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
