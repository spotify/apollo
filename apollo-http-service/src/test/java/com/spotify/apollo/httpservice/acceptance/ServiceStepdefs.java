/*
 * -\-\-
 * Spotify Apollo HTTP Service
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
package com.spotify.apollo.httpservice.acceptance;

import com.google.common.base.Splitter;

import com.spotify.apollo.AppInit;
import com.spotify.apollo.Request;
import com.spotify.apollo.Response;
import com.spotify.apollo.test.ServiceHelper;
import com.spotify.apollo.test.response.ResponseWithDelay;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import cucumber.api.java.After;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import okio.ByteString;

import static com.google.common.collect.Iterables.toArray;
import static com.spotify.apollo.test.unit.RequestMatchers.uri;
import static org.apache.commons.lang.StringUtils.reverse;
import static org.hamcrest.CoreMatchers.startsWith;
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
        .forwardingNonStubbedRequests(false)
        .conf("reverser.address", AcceptanceIT.REVERSER_ADDRESS);

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

  @And("^a running reverser service$")
  public void aRunningReverserService() throws Throwable {
    serviceHelper.stubClient()
        .respond(request -> ResponseWithDelay.forResponse(Response.forPayload(reverseArg(request))))
        .to(uri(startsWith(AcceptanceIT.REVERSER_ADDRESS)));
  }

  interface BootedApplication {
    Optional<String> pod();
  }

  private static String httpPort(String port) {
    return " -Dhttp.server.port=" + port;
  }

  private ByteString reverseArg(Request request) {
    return ByteString.encodeUtf8(reverse(request.parameter("arg").orElse("no arg!!")));
  }
}
