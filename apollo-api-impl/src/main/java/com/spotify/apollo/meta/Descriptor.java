/**
 * Copyright (C) 2012 Spotify AB
 */

package com.spotify.apollo.meta;

import com.google.auto.value.AutoValue;

/**
 * Describes an application artifact in maven terms, i.e. groupId, artifactId and version.
 */
@AutoValue
public abstract class Descriptor {

  /**
   * @return The groupId.
   */
  public abstract String groupId();

  /**
   * @return The artifactId.
   */
  public abstract String artifactId();

  /**
   * @return The version.
   */
  public abstract String version();

  /**
   * Create a new application artifact descriptor.
   */
  public static Descriptor create(String groupId, String artifactId, String version) {
    return new AutoValue_Descriptor(groupId, artifactId, version);
  }
}
