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
package com.spotify.apollo.meta;

import com.spotify.apollo.Environment;
import com.spotify.apollo.Response;
import com.spotify.apollo.test.ServiceHelper;
import com.spotify.apollo.meta.model.Meta;
import com.spotify.apollo.meta.model.MetaGatherer;
import com.spotify.apollo.meta.model.MetaInfoBuilder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import org.junit.After;
import org.junit.Test;

import okio.ByteString;

import static com.spotify.apollo.meta.model.Model.MetaInfo;
import static com.spotify.apollo.test.unit.StatusTypeMatchers.withCode;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

public class MetaApplicationTest {

  private static final String API_BASE = "http://meta-application/_meta/0";

  private static final String BUILD_VERSION = "3.2.1-SNAPSHOT";
  private static final String CONTAINER_VERSION = "apollo-standalone 0.5.0";

  private static final String CONFIG_DEFAULT = "integration-test";
  private static final String CONFIG_NO_EXPOSE = "integration-test-no-expose";

  private static final MetaInfo META_INFO = new MetaInfoBuilder()
      .componentId("meta-application")
      .buildVersion(BUILD_VERSION)
      .containerVersion(CONTAINER_VERSION)
      .build();

  private ServiceHelper serviceHelper = ServiceHelper.create(this::create, "meta-application")
      .disableMetaApi();

  private MetaGatherer gatherer;
  private MetaApplication app;

  void create(Environment environment) {
    environment.routingEngine()
        .registerAutoRoutes(app);
  }

  @After
  public void tearDown() throws Exception {
    serviceHelper.close();
  }

  private void setUpWith(String configName) throws InterruptedException {
    Config config = ConfigFactory.load(configName);
    gatherer = Meta.createGatherer(META_INFO, config);

    app = new MetaApplication(gatherer);
    serviceHelper.start();
  }

  @Test
  public void shouldRespondWithInfo() throws Exception {
    setUpWith(CONFIG_DEFAULT);
    Response<ByteString> response = serviceHelper.request("GET", API_BASE + "/info")
        .toCompletableFuture().get();

    assertThat(response.status(), withCode(200));

    String responseJson = response.payload().get().utf8();

    assertThat("is wrapped in result", responseJson, containsString("{\"result\":{"));

    assertThat(responseJson, containsString(BUILD_VERSION));
    assertThat(responseJson, containsString(CONTAINER_VERSION));
    assertThat(responseJson, containsString(System.getProperty("java.version")));
    assertThat(responseJson, containsString("\"buildVersion\":"));
    assertThat(responseJson, containsString("\"containerVersion\":"));
    assertThat(responseJson, containsString("\"systemVersion\":"));
    assertThat(responseJson, containsString("\"serviceUptime\":"));
  }

  @Test
  public void shouldRespondWithDisabledConfig() throws Exception {
    setUpWith(CONFIG_NO_EXPOSE);
    Response<ByteString> response = serviceHelper.request("GET", API_BASE + "/config")
        .toCompletableFuture().get();

    assertThat(response.status(), withCode(200));
    String responseJson = response.payload().get().utf8();

    assertThat("is wrapped in result.spNode", responseJson,
                      containsString("{\"result\":{\"spNode\":{"));

    assertThat("doesn't contain config value", responseJson,
                      allOf(not(containsString("foo")),
                                     not(containsString("num")),
                                     not(containsString("123")),
                                     not(containsString("str")),
                                     not(containsString("bla")),
                                     not(containsString("list"))
                      ));

    assertThat("has enabling note", responseJson,
                      allOf(containsString("disabled"),
                                     containsString("enable by adding")
                      ));
  }

  @Test
  public void shouldRespondWithConfig() throws Exception {
    setUpWith(CONFIG_DEFAULT);
    Response<ByteString> response = serviceHelper.request("GET", API_BASE + "/config")
        .toCompletableFuture().get();

    assertThat(response.status(), withCode(200));
    String responseJson = response.payload().get().utf8();

    assertThat("is wrapped in result.spNode", responseJson,
                      containsString("{\"result\":{\"spNode\":{"));

    assertThat("contains config values", responseJson,
                      allOf(containsString("\"foo\":{"),
                            containsString("\"num\":123"),
                            containsString("\"str\":\"bla\""),
                            containsString("\"list\":[1,2,3]")
                      ));

    assertThat("secrets are filtered", responseJson, containsString("\"hidden\":{\"bowman\":\"*"));
    assertThat("secrets are filtered", responseJson, containsString("\"a-bowman-list\":\"*"));
               assertThat("secrets are filtered", responseJson, containsString("\"password\":\"*"));
    assertThat("secrets are filtered", responseJson, not(containsString("deadbeef")));
    assertThat("secrets are filtered", responseJson, not(containsString("[5,3,0,2,3,7]")));
    assertThat("secrets are filtered", responseJson, not(containsString("s3cr3t")));

    assertThat("_meta is not filtered", responseJson,
                      allOf(containsString("\"expose-config\":true"),
                                     containsString("\"config-filter\":{"),
                                     containsString("\"bowman\":true"),
                                     containsString("\"str\":false"),
                                     containsString("\"num\":\"visible\"")
                      ));
  }

  @Test
  public void shouldRespondWithConfigOrigins() throws Exception {
    setUpWith(CONFIG_DEFAULT);
    Response<ByteString> response = serviceHelper.request("GET", API_BASE + "/config?origins")
        .toCompletableFuture().get();

    assertThat(response.status(), withCode(200));
    String responseJson = response.payload().get().utf8();

    assertThat("is wrapped in result.spNode", responseJson,
                      containsString("{\"result\":{\"spNode\":{"));

    assertThat("contains config origins", responseJson,
                      allOf(containsString("\"foo__origin\":\"integration-test"),
                                     containsString("\"num__origin\":\"integration-test"),
                                     containsString("\"str__origin\":\"integration-test"),
                                     containsString("\"list__origin\":\"integration-test")
                      ));

    assertThat("contains config values", responseJson,
                      allOf(containsString("\"foo\":{"),
                                     containsString("\"num\":123"),
                                     containsString("\"str\":\"bla\""),
                                     containsString("\"list\":[1,2,3]")
                      ));

    assertThat("secrets are filtered", responseJson,
                      allOf(not(containsString("deadbeef")),
                                     not(containsString("[5,3,0,2,3,7]")),
                                     not(containsString("s3cr3t"))
                      ));

    assertThat("contains fileted origin", responseJson,
                      containsString(
                          "\"bowman__origin\":\"filtered by config-filter settings\""));

    assertThat("_meta is not filtered", responseJson,
                      allOf(containsString("\"expose-config\":true"),
                                     containsString("\"config-filter\":{"),
                                     containsString("\"bowman\":true"),
                                     containsString("\"str\":false"),
                                     containsString("\"num\":\"visible\"")
                      ));
  }

  @Test
  public void shouldRespondWithEndpoints() throws Exception {
    setUpWith(CONFIG_DEFAULT);

    MetaGatherer.CallsGatherer endpointsGatherer = gatherer.getServiceCallsGatherer();
    endpointsGatherer.setDocstring("Service docstring - tells you what I do");
    call(endpointsGatherer);

    Response<ByteString> response = serviceHelper.request("GET", API_BASE + "/endpoints")
        .toCompletableFuture().get();

    assertThat(response.status(), withCode(200));
    String responseJson = response.payload().get().utf8();

    assertThat("is wrapped in result", responseJson,
                      containsString("{\"result\":{"));

    assertThat("has endpoints list", responseJson,
                      containsString("\"endpoints\":["));

    assertCall(responseJson);
  }

  @Test
  public void shouldRespondWithCalls() throws Exception {
    setUpWith(CONFIG_DEFAULT);

    MetaGatherer.CallsGatherer incomingGatherer = gatherer.getIncomingCallsGatherer("bar");
    MetaGatherer.CallsGatherer outgoingGatherer = gatherer.getOutgoingCallsGatherer("baz");
    call(incomingGatherer);
    call(outgoingGatherer);

    Response<ByteString> response = serviceHelper.request("GET", API_BASE + "/calls")
        .toCompletableFuture().get();

    assertThat(response.status(), withCode(200));
    String responseJson = response.payload().get().utf8();

    assertThat("is wrapped in result", responseJson,
                      containsString("{\"result\":{"));

    assertThat("has calls", responseJson,
                      allOf(containsString("\"incoming\":{\"bar\":"),
                                     containsString("\"outgoing\":{\"baz\":")
                      ));

    assertCall(responseJson);

    String[] parts = responseJson.split("incoming|outgoing");
    assertThat(parts.length, is(3));
    for (int i = 1; i < parts.length; ++i) {
      assertCall(parts[i]);
    }
  }

  private static void assertCall(String s) {
    assertThat(s, containsString("\"methodName\":\"someName\""));
    assertThat(s, containsString("\"method\":[\"GET\"]"));
    assertThat(s, containsString("\"queryParameters\":[{\"name\":\"q\"}]"));
  }

  private static void call(MetaGatherer.CallsGatherer callsGatherer) {
    MetaGatherer.EndpointGatherer endpointGatherer = callsGatherer.namedEndpointGatherer("someName");
    endpointGatherer.setUri("/foo-uri/bla");
    endpointGatherer.addMethod("GET");
    endpointGatherer.addQueryParameterName("q");
    endpointGatherer.setRequestContentType("req-type");
    endpointGatherer.setResponseContentType("res-type");
  }
}
