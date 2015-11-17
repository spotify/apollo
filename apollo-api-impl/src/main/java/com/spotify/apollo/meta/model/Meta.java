/*
 * Copyright © 2014 Spotify AB
 */
package com.spotify.apollo.meta.model;

import com.typesafe.config.Config;

/**
 * Factory methods for creating metadata gatherers.
 */
public final class Meta {

  private Meta() {
    // no instantiation
  }

  public static MetaGatherer createGatherer(Model.MetaInfo metaInfo) {
    return new DefaultMetaGatherer(metaInfo);
  }

  public static MetaGatherer createGatherer(Model.MetaInfo metaInfo, Config config) {
    return new DefaultMetaGatherer(metaInfo, config);
  }

}
