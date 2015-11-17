/*
 * Copyright (c) 2014 Spotify AB
 */

package com.spotify.apollo.httpservice.acceptance;

import com.google.common.base.Splitter;

import com.spotify.apollo.AppInit;
import com.spotify.apollo.test.ServiceHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import cucumber.api.java.After;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;

import static com.google.common.collect.Iterables.toArray;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ServiceStepdefs {

  private static final Logger LOG = LoggerFactory.getLogger(ServiceStepdefs.class);
  private static final Splitter SPACE_SPLITTER = Splitter.on(' ');

  static ServiceHelper serviceHelper = null;
  BootedApplication bootedApplication = null;

  @Given("^the \"([^\"]*)\" service started in pod \"([^\"]*)\" on port \"([^\"]*)\"$")
  public void service_in_pod(final String serviceName,
                             final String pod,
                             final String port) throws Throwable {
    final SimpleService simpleService = new SimpleService();


    startService(serviceName, pod, "", port, simpleService);

    bootedApplication = simpleService;
  }

  @Given("^the \"([^\"]*)\" service started in pod \"([^\"]*)\" with args \"([^\"]*)\" on port \"([^\"]*)\"$")
  public void service_with_args(final String serviceName,
                                final String pod,
                                final String args,
                                final String port) throws Throwable {
    final SimpleService simpleService = new SimpleService();

    startService(serviceName, pod, args, port, simpleService);

    bootedApplication = simpleService;
  }

  @Given("^the \"([^\"]*)\" blessed-path service started in pod \"([^\"]*)\" on port \"([^\"]*)\"$")
  public void blessed_service_with_args(final String serviceName,
                                      final String pod,
                                      final String port) throws Throwable {
    startService(serviceName, pod, "", port, new BlessedPathService());
  }

  private synchronized void startService(final String serviceName,
                                         final String pod,
                                         final String args,
                                         final String port,
                                         final AppInit service) throws InterruptedException {
    if (serviceHelper != null) {
      LOG.info("Already running an application; not starting another");
      return;
    }

    final String[] allArgs =
        toArray(SPACE_SPLITTER.split(args + httpPort(port)), String.class);

    serviceHelper = ServiceHelper.create(service, serviceName)
        .args(allArgs)
        .domain(pod)
        .forwardingNonStubbedRequests(false);

    serviceHelper.start();
  }

  @And("^application should have started in pod \"([^\"]*)\"$")
  public void application_should_have_started_with_pod(String pod) throws Throwable {
    assertTrue(bootedApplication.pod().isPresent());
    assertEquals(pod, bootedApplication.pod().get());
  }

  @After
  public void tearDown() throws Throwable {
    serviceHelper.close();
    serviceHelper = null;
    bootedApplication = null;
  }

  interface BootedApplication {
    Optional<String> pod();
  }

  private static String httpPort(String port) {
    return " -Dhttp.server.port=" + port;
  }

}
