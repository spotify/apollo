/*
 * Copyright (c) 2014 Spotify AB
 */

package com.spotify.apollo.standalone;

import static com.spotify.apollo.core.Service.Instance;

public interface InstanceListener {
  void instanceCreated(Instance instance);
}
