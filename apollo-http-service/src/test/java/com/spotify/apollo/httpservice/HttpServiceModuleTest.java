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
package com.spotify.apollo.httpservice;

import com.spotify.apollo.AppInit;
import com.spotify.apollo.Environment;
import com.spotify.apollo.core.Service;
import com.spotify.apollo.core.Services;
import com.spotify.apollo.request.RequestHandler;

import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class HttpServiceModuleTest {

  @Test
  public void shouldInitAndDestroyApplication() throws Exception {
    final AtomicBoolean init = new AtomicBoolean();
    final AtomicBoolean destroy = new AtomicBoolean();
    final App app = new App(init, destroy);

    try (Service.Instance i = service(app).start()) {
      final RequestHandler handler = HttpServiceModule.requestHandler(i);
      assertNotNull(handler);
    } catch (IOException e) {
      fail(e.getMessage());
    }

    assertTrue(init.get());
    assertTrue(destroy.get());
  }

  @Test
  public void shouldDestroyApplicationOnExit() throws Exception {
    final AtomicBoolean init = new AtomicBoolean();
    final AtomicBoolean destroy = new AtomicBoolean();
    final App app = new App(init, destroy);

    try (Service.Instance i = service(app).start()) {
      final RequestHandler handler = HttpServiceModule.requestHandler(i);
      assertNotNull(handler);

      // not calling environment.close()
    } catch (IOException e) {
      fail(e.getMessage());
    }

    assertTrue(init.get());
    assertTrue(destroy.get());
  }

  public Service service(AppInit app) {
    return Services.usingName("test")
        .withModule(HttpServiceModule.create(app))
        .build();
  }

  private static class App implements AppInit {

    private final AtomicBoolean init;
    private final AtomicBoolean destroy;

    App(AtomicBoolean init, AtomicBoolean destroy) {
      this.init = init;
      this.destroy = destroy;
    }

    @Override
    public void create(Environment environment) {
      assertNotNull(environment);
      assertTrue(init.compareAndSet(false, true));
      environment.closer().register(this::destroy);
    }

    public void destroy() {
      assertTrue(destroy.compareAndSet(false, true));
    }
  }
}
