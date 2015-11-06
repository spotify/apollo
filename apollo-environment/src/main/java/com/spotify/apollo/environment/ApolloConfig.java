/*
 * Copyright (c) 2013-2015 Spotify AB
 */

package com.spotify.apollo.environment;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.util.Objects;

import javax.inject.Inject;

import static com.spotify.apollo.environment.ConfigUtil.optionalBoolean;
import static com.spotify.apollo.environment.ConfigUtil.optionalString;
import static com.spotify.apollo.environment.ConfigUtil.either;

/**
 * Configuration object for keys under the apollo keyspace
 */
public class ApolloConfig {

  private final Config apolloNode;

  @Inject
  public ApolloConfig(Config configNode) {
    final Config root = Objects.requireNonNull(configNode);

    apolloNode = root.hasPath("apollo")
        ? root.getConfig("apollo")
        : ConfigFactory.empty();
  }

  public String backend() {
    return either(optionalString(apolloNode, "domain"),
                  optionalString(apolloNode, "backend"))
        .orElse("");
  }

  public boolean enableIncomingRequestLogging() {
    return optionalBoolean(apolloNode, "logIncomingRequests").orElse(true);
  }

  public boolean enableMetaApi() {
    return optionalBoolean(apolloNode, "metaApi").orElse(true);
  }
}
