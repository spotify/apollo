/*
 * -\-\-
 * Spotify Apollo API Implementations
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
