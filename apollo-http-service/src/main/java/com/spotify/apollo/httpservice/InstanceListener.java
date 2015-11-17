/*
 * Copyright (c) 2014 Spotify AB
 */

package com.spotify.apollo.httpservice;

import static com.spotify.apollo.core.Service.Instance;

@FunctionalInterface
public interface InstanceListener {
  void instanceCreated(Instance instance);
}
