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

import com.google.common.io.Closer;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.name.Names;

import com.spotify.apollo.core.Services;
import com.spotify.apollo.module.AbstractApolloModule;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Collections.singletonMap;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class HttpServerProviderTest {

  @Test
  public void shouldCloseThingsInTheRightOrder() throws Exception {
    AtomicInteger counter = new AtomicInteger();
    Closer closer = Closer.create();
    Config config = ConfigFactory.parseMap(singletonMap("http.server.port", "9999"));

    // server close should happen first: counter goes from 0 to 1
    Runnable onClose = () -> counter.compareAndSet(0, 1);

    Injector injector = Guice.createInjector(
        new TestSetup(config, closer),
        HttpServerModule.createForTest(onClose));
    HttpServer instance = injector.getInstance(HttpServer.class);

    // something that registered before start should close after server: counter goes from 1 to 2
    closer.register(() -> counter.compareAndSet(1, 2));

    // start server then close the closer
    instance.start(request -> {});
    closer.close();

    // counter should reach two if things are closed in the right order
    assertThat(counter.get(), is(2));
  }

  static class TestSetup extends AbstractApolloModule {

    private final Config config;
    private final Closer closer;

    TestSetup(Config config, Closer closer) {
      this.config = config;
      this.closer = closer;
    }

    @Override
    protected void configure() {
      bind(Config.class).toInstance(config);
      bind(Closer.class).toInstance(closer);
      bindConstant().annotatedWith(Names.named(Services.INJECT_SERVICE_NAME)).to("test");
    }

    @Override
    public String getId() {
      return "test";
    }
  }
}
