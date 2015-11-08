/*
 * Copyright © 2014 Spotify AB
 */
package com.spotify.apollo.standalone.acceptance;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cucumber.api.junit.Cucumber;

@RunWith(Cucumber.class)
public class AcceptanceIT {

  private static final Logger log = LoggerFactory.getLogger(AcceptanceIT.class);

  @BeforeClass
  public static synchronized void start() throws Exception {
    log.info("AcceptanceIT start()");
  }
}
