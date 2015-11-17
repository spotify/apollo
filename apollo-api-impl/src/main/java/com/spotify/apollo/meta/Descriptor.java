/**
 * Copyright (C) 2012 Spotify AB
 */

package com.spotify.apollo.meta;

import com.google.auto.value.AutoValue;

/**
 * Describes an application with serviceName and version.
 */
@AutoValue
public abstract class Descriptor {

  /**
   * @return The serviceName.
   */
  public abstract String serviceName();

  /**
   * @return The version.
   */
  public abstract String version();

  /**
   * Create a new application artifact descriptor.
   */
  public static Descriptor create(String serviceName, String version) {
    return new AutoValue_Descriptor(serviceName, version);
  }
}
