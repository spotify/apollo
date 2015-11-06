/*
 * Copyright (c) 2013-2014 Spotify AB
 */

package com.spotify.apollo.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThreadExceptionHandler implements Thread.UncaughtExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(ThreadExceptionHandler.class);

  @Override
  public void uncaughtException(Thread t, Throwable e) {
    LOG.error("Uncaught exception", e);
  }
}
