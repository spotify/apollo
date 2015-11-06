package com.spotify.apollo.standalone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class LogAndExit implements Thread.UncaughtExceptionHandler {

  // note: using StandaloneService as the class; that's more meaningful than LogAndExit.
  private static final Logger LOG = LoggerFactory.getLogger(StandaloneService.class);

  @Override
  public void uncaughtException(Thread t, Throwable e) {
    LOG.error("Uncaught exception on thread {}, exiting", t, e);
    System.exit(1);
  }
}
