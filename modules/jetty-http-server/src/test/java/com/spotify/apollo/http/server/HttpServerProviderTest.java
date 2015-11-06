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
