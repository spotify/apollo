package com.spotify.apollo.example;

import com.spotify.apollo.Response;
import com.spotify.apollo.core.Service;
import com.spotify.apollo.core.Services;
import com.spotify.apollo.http.server.HttpServerModule;
import com.spotify.apollo.request.OngoingRequest;
import com.spotify.apollo.request.RequestHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

final class App {

  private static final Logger LOG = LoggerFactory.getLogger(App.class);

  public static void main(String[] args) throws IOException, InterruptedException {
    Service service = Services.usingName("test")
        .withModule(HttpServerModule.create())
        .build();

    try (Service.Instance instance = service.start(args)) {
      final RequestHandler handler = new Handler();
      HttpServerModule.server(instance).start(handler);

      LOG.info("Started service '{}'", service.getServiceName());

      final String string = instance.getConfig().getString("config.key");
      LOG.info("Value of config.key: {}", string);

      instance.waitForShutdown();
      LOG.info("Stopping service '{}'", service.getServiceName());
    }
  }

  static class Handler implements RequestHandler {

    Handler() {
      LOG.info("Creating Handler");
    }

    @Override
    public void handle(OngoingRequest request) {
      request.reply(Response.ok());
    }
  }
}
