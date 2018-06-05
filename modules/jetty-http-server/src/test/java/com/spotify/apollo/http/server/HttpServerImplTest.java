/*
 * -\-\-
 * Spotify Apollo Jetty HTTP Server Module
 * --
 * Copyright (C) 2013 - 2018 Spotify AB
 * --
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at

 *      http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -/-/-
 */
package com.spotify.apollo.http.server;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableMap;
import com.spotify.apollo.core.Service;
import com.spotify.apollo.core.Services;
import com.spotify.apollo.http.common.TestHandler;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.io.IOException;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.junit.Test;

public class HttpServerImplTest {

  Config withMaxThreads(int maxThreads, int port) {
    return ConfigFactory.parseMap(
        ImmutableMap.of(
            "http.server.maxThreads", Integer.toString(maxThreads),
            "http.server.port", Integer.toString(port)
        )
    );
  }

  @Test
  public void setsMaxThreadsOnJettyServer() throws IOException {
    int maxThreads = 30;
    int port = 9086;

    final Service service = Services.usingName("test")
        .withModule(HttpServerModule.create())
        .build();

    Service.Instance instance = service.start(new String[0], withMaxThreads(maxThreads,port));

    try {
      HttpServerImpl server = (HttpServerImpl) HttpServerModule.server(instance);
      TestHandler testHandler = new TestHandler();

      server.start(testHandler);

      QueuedThreadPool threadPool = (QueuedThreadPool) server.server.getThreadPool();
      assertThat(threadPool.getMaxThreads(), is(maxThreads));
      server.close();
    } finally {
      instance.close();
    }
  }
}
