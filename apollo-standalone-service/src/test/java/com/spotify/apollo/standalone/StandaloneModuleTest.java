package com.spotify.apollo.standalone;

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

public class StandaloneModuleTest {

  @Test
  public void shouldInitAndDestroyApplication() throws Exception {
    final AtomicBoolean init = new AtomicBoolean();
    final AtomicBoolean destroy = new AtomicBoolean();
    final App app = new App(init, destroy);

    try (Service.Instance i = service(app).start()) {
      final RequestHandler handler = StandaloneModule.requestHandler(i);
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
      final RequestHandler handler = StandaloneModule.requestHandler(i);
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
        .withModule(StandaloneModule.create(app))
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
