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
import com.spotify.apollo.Client;
import com.spotify.apollo.core.Service;
import com.spotify.apollo.core.Services;
import com.spotify.apollo.environment.ApolloConfig;
import com.spotify.apollo.http.client.HttpClientModule;
import com.spotify.apollo.http.server.HttpServerModule;
import com.spotify.apollo.meta.MetaDescriptor;
import com.spotify.apollo.metrics.MetricsModule;
import com.spotify.apollo.request.RequestHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;

import static java.lang.String.format;

/**
 * A standard setup for and apollo http-based api service.
 *
 * The setup uses {@link HttpServerModule} for binding the api {@link RequestHandler} and
 * {@link HttpClientModule} to add http request support to the service {@link Client}.
 */
public final class HttpService {

  private static final Logger LOG = LoggerFactory.getLogger(HttpService.class);

  private HttpService() {
  }

  public static Service.Builder usingAppInit(
      AppInit appInit,
      String serviceName) {

    return builder(serviceName)
        .withModule(HttpServiceModule.create(appInit));
  }

  public static Service.Builder builder(String serviceName) {
    return Services.usingName(serviceName)
        .usingModuleDiscovery(false)
        .withModule(MetricsModule.create())
        .withModule(MetricIdModule.create())
        .withModule(HttpClientModule.create())
        .withModule(HttpServerModule.create());
  }

  public static void boot(AppInit appInit,
                          String serviceName,
                          String... args) throws LoadingException {
    boot(appInit, serviceName, noopListener(), args);
  }

  public static void boot(AppInit appInit,
                          String serviceName,
                          InstanceListener instanceListener,
                          String... args) throws LoadingException {
    final Service service = usingAppInit(appInit, serviceName).build();

    boot(service, instanceListener, args);
  }

  public static void boot(Service service, String... args) throws LoadingException {
    boot(service, noopListener(), args);
  }

  public static void boot(Service service,
                          InstanceListener instanceListener,
                          String... args) throws LoadingException {
    boot(service, instanceListener, new LogAndExit(), args);
  }

  /**
   * Boot up a service and wait for it to shut down.
   *
   * @param service the service to start
   * @param instanceListener gets called when a service instance has been created
   * @param uncaughtExceptionHandler an exception handler that gets invoked for the current thread
   *                                 if any uncaught exceptions are thrown during service startup
   *                                 or while waiting for it to shut down.
   * @param args arguments to the service
   * @throws LoadingException in case of an error starting up
   */
  public static void boot(Service service,
                          InstanceListener instanceListener,
                          Thread.UncaughtExceptionHandler uncaughtExceptionHandler,
                          String... args) throws LoadingException {

    Objects.requireNonNull(uncaughtExceptionHandler);
    Thread.currentThread().setUncaughtExceptionHandler(uncaughtExceptionHandler);

    LOG.debug("Trying to create instance of service {} with args {}",
              service.getServiceName(), args);

    try (Service.Instance instance = service.start(args)) {
      final RequestHandler requestHandler = HttpServiceModule.requestHandler(instance);

      HttpServerModule.server(instance).start(requestHandler);

      final String serviceName = service.getServiceName();
      final MetaDescriptor metaDescriptor = instance.resolve(MetaDescriptor.class);
      final ApolloConfig config = instance.resolve(ApolloConfig.class);

      LOG.info("Started {} {} (apollo {}) with backend domain '{}'",
               serviceName,
               metaDescriptor.descriptor().version(),
               metaDescriptor.apolloVersion(),
               config.backend());

      if (instanceListener != null) {
        instanceListener.instanceCreated(instance);
      }

      instance.waitForShutdown();

      LOG.info("Starting shutdown of {} ...", serviceName);
    } catch (IOException e) {
      throw failure(e, "Failed to start service");
    } catch (InterruptedException e) {
      throw failure(e, "Service interrupted");
    } catch (Exception e) {
      throw failure(e, "Something went wrong");
    }

    LOG.info("Shutdown of {} complete", service.getServiceName());
  }

  static LoadingException failure(Throwable cause, String message, Object... args) {
    return new LoadingException(format(message, args), cause);
  }

  static InstanceListener noopListener() {
    return instance -> {};
  }
}
