/*
 * Copyright © 2006-2015 Spotify AB
 */
package com.spotify.apollo;

/**
 * An application initializer that will set up the application using an {@link Environment}.
 *
 * A typical application will read the {@link Environment#config()} and set up any application
 * specific resources. These resources should be registered with the {@link Environment#closer()}
 * in order to be properly closed when shutting down.
 */
public interface AppInit {

  /**
   * Sets up an application.
   *
   * @param environment  The Environment in which the application should be initialized
   */
  void create(Environment environment);
}
