/*
 * Copyright (c) 2014 Spotify AB
 */

package com.spotify.apollo.route;

import java.util.stream.Stream;

/**
 * A programmatic way of defining {@link Route}s in an Apollo service.
 */
public interface RouteProvider {
  Stream<? extends Route<? extends AsyncHandler<?>>> routes();
}
