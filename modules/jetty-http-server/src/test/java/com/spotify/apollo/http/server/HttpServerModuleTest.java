/*
 * -\-\-
 * Spotify Apollo Jetty HTTP Server Module
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
package com.spotify.apollo.http.server;

import com.google.common.collect.Lists;

import com.spotify.apollo.Response;
import com.spotify.apollo.core.Service;
import com.spotify.apollo.core.Services;
import com.spotify.apollo.request.OngoingRequest;
import com.spotify.apollo.request.RequestHandler;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.util.List;
import java.util.Optional;

import static com.spotify.apollo.Status.IM_A_TEAPOT;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class HttpServerModuleTest {

  private static final String[] NO_ARGS = new String[0];

  @Rule
  public ExpectedException exception = ExpectedException.none();

  private OkHttpClient okHttpClient = new OkHttpClient();

  @Test
  public void testCanStartRegularModule() throws Exception {
    int port = 9083;

    try (Service.Instance instance = service(onPort(port)).start(NO_ARGS)) {
      HttpServer server = HttpServerModule.server(instance);
      assertCanNotConnect(port);

      TestHandler testHandler = new TestHandler();
      server.start(testHandler);
      assertCanConnect(port);

      Request request = new Request.Builder().get()
          .url(baseUrl(port) + "/hello/world")
          .build();

      com.squareup.okhttp.Response response = okHttpClient.newCall(request).execute();
      assertThat(response.code(), is(IM_A_TEAPOT.code()));

      assertThat(testHandler.requests.size(), is(1));
      OngoingRequest incomingRequest = testHandler.requests.get(0);
      assertThat(incomingRequest.request().uri(), is("/hello/world"));

      server.close();
      assertCanNotConnect(port);
    }
  }

  @Test
  public void testParsesQueryParameters() throws Exception {
    int port = 9084;

    try (Service.Instance instance = service(onPort(port)).start(NO_ARGS)) {
      HttpServer server = HttpServerModule.server(instance);
      TestHandler testHandler = new TestHandler();
      server.start(testHandler);

      Request httpRequest = new Request.Builder().get()
          .url(baseUrl(port) + "/query?a=foo&b=bar&b=baz")
          .build();

      com.squareup.okhttp.Response response = okHttpClient.newCall(httpRequest).execute();
      assertThat(response.code(), is(IM_A_TEAPOT.code()));

      assertThat(testHandler.requests.size(), is(1));
      final com.spotify.apollo.Request apolloRequest = testHandler.requests.get(0).request();
      assertThat(apolloRequest.uri(), is("/query?a=foo&b=bar&b=baz"));
      assertThat(apolloRequest.parameter("a"), is(Optional.of("foo")));
      assertThat(apolloRequest.parameter("b"), is(Optional.of("bar")));
      assertThat(apolloRequest.parameters().get("b"), is(asList("bar", "baz")));
      assertThat(apolloRequest.parameter("c"), is(Optional.empty()));
    }
  }

  @Test
  public void testParsesHeadersParameters() throws Exception {
    int port = 9085;

    try (Service.Instance instance = service(onPort(port)).start(NO_ARGS)) {
      HttpServer server = HttpServerModule.server(instance);
      TestHandler testHandler = new TestHandler();
      server.start(testHandler);

      Request httpRequest = new Request.Builder().get()
          .url(baseUrl(port) + "/headers")
          .addHeader("Foo", "bar")
          .addHeader("Repeat", "once")
          .addHeader("Repeat", "twice")
          .build();

      com.squareup.okhttp.Response response = okHttpClient.newCall(httpRequest).execute();
      assertThat(response.code(), is(IM_A_TEAPOT.code()));

      assertThat(testHandler.requests.size(), is(1));
      final com.spotify.apollo.Request apolloRequest = testHandler.requests.get(0).request();
      assertThat(apolloRequest.uri(), is("/headers"));
      assertThat(apolloRequest.header("Foo"), is(Optional.of("bar")));
      assertThat(apolloRequest.header("Repeat"), is(Optional.of("once,twice")));
      assertThat(apolloRequest.header("Baz"), is(Optional.empty()));

      System.out.println("apolloRequest.headers() = " + apolloRequest.headers());
    }
  }

  Service service(JettyHttpServerConfiguration configuration) {
    return Services.usingName("test")
        .withModule(HttpServerModule.create(configuration))
        .build();
  }

  JettyHttpServerConfiguration onPort(int port) {
    return JettyHttpServerConfiguration.create().withPort(port);
  }

  String baseUrl(int port) {
    return "http://localhost:" + port;
  }

  private static void assertCanConnect(int port) throws IOException {
    try (Socket localhost = new Socket("localhost", port)) {
      assertTrue(localhost.isConnected());
    }
  }

  private static void assertCanNotConnect(int port) throws IOException {
    try (Socket ignored = new Socket("localhost", port)) {
      fail();
    } catch (ConnectException e) {
      assertThat(e.getMessage(), containsString("Connection refused"));
    }
  }

  private static class TestHandler implements RequestHandler {

    List<OngoingRequest> requests = Lists.newLinkedList();

    @Override
    public void handle(OngoingRequest request) {
      requests.add(request);
      request.reply(Response.forStatus(IM_A_TEAPOT));
    }
  }
}
