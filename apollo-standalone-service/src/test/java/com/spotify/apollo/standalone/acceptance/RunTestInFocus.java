/*
 * Copyright © 2014 Spotify AB
 */
package com.spotify.apollo.standalone.acceptance;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;

@RunWith(Cucumber.class)
@CucumberOptions(tags = {"@Focus"})
public class RunTestInFocus {

  private static final Logger log = LoggerFactory.getLogger(RunTestInFocus.class);

  @BeforeClass
  public static synchronized void start() throws Exception {
    log.info("TestInFocus start()");
  }
}
