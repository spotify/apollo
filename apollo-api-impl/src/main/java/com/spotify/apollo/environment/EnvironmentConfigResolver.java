/*
 * Copyright (c) 2014 Spotify AB
 */

package com.spotify.apollo.environment;

import com.typesafe.config.Config;

public interface EnvironmentConfigResolver {

  Config getConfig(String serviceName);

}
