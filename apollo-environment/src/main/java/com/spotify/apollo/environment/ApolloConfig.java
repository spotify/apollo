/*
 * -\-\-
 * Spotify Apollo API Environment
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
package com.spotify.apollo.environment;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.util.Objects;

import javax.inject.Inject;

import static com.spotify.apollo.environment.ConfigUtil.either;
import static com.spotify.apollo.environment.ConfigUtil.optionalBoolean;
import static com.spotify.apollo.environment.ConfigUtil.optionalString;

/**
 * Configuration object for keys under the apollo keyspace
 */
public class ApolloConfig {

  private final Config root;
  private final Config apolloNode;

  @Inject
  public ApolloConfig(Config configNode) {
    root = Objects.requireNonNull(configNode);

    apolloNode = root.hasPath("apollo")
                 ? root.getConfig("apollo")
                 : ConfigFactory.empty();
  }

  public String backend() {
    return either(either(optionalString(apolloNode, "domain"),
                         optionalString(apolloNode, "backend")),
                  optionalString(root, "domain"))
        .orElse("");
  }

  public boolean enableIncomingRequestLogging() {
    return optionalBoolean(apolloNode, "logIncomingRequests").orElse(true);
  }

  public boolean enableOutgoingRequestLogging() {
    return optionalBoolean(apolloNode, "logOutgoingRequests").orElse(true);
  }

  public boolean enableMetaApi() {
    return optionalBoolean(apolloNode, "metaApi").orElse(true);
  }
}
