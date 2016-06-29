/*
 * -\-\-
 * Spotify Apollo Metrics Module
 * --
 * Copyright (C) 2013 - 2016 Spotify AB
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
package com.spotify.apollo.metrics;

import com.google.auto.service.AutoService;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;

import com.spotify.apollo.core.Services;
import com.spotify.apollo.environment.EndpointRunnableFactoryDecorator;
import com.spotify.apollo.module.AbstractApolloModule;
import com.spotify.apollo.module.ApolloModule;
import com.spotify.apollo.metrics.semantic.SemanticApolloMetrics;
import com.spotify.metrics.core.MetricId;
import com.spotify.metrics.core.SemanticMetricRegistry;
import com.spotify.metrics.ffwd.FastForwardReporter;
import com.spotify.metrics.jvm.CpuGaugeSet;
import com.spotify.metrics.jvm.GarbageCollectorMetricSet;
import com.spotify.metrics.jvm.MemoryUsageGaugeSet;
import com.spotify.metrics.jvm.ThreadStatesMetricSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.inject.Named;

/**
 * Provides bindings to {@link SemanticMetricRegistry} and {@link ApolloMetrics}.
 *
 * A {@link FastForwardReporter} will be configured and started according to the
 * {@link FfwdConfig} schema.
 *
 * Currently this module requires an external binding for a {@link MetricId} that
 * will be used as the ffwd prefix.
 */
// we're OK with using Optional fields and parameters in this class
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@AutoService(ApolloModule.class)
public class MetricsModule extends AbstractApolloModule {

  private static final Logger LOG = LoggerFactory.getLogger(MetricsModule.class);

  private final Optional<MetricId> metricId;

  private MetricsModule(Optional<MetricId> metricId) {
    this.metricId = metricId;
  }

  // Visible for SPI support
  public MetricsModule() {
    this(Optional.empty());
  }

  public static MetricsModule create() {
    return new MetricsModule(Optional.empty());
  }

  public static MetricsModule forMetricId(MetricId metricId) {
    return new MetricsModule(Optional.of(metricId));
  }

  @Override
  protected void configure() {
    bind(FfwdConfig.class);

    if (metricId.isPresent()) {
      bind(MetricId.class).toInstance(metricId.get());
    }

    Multibinder.newSetBinder(binder(), EndpointRunnableFactoryDecorator.class)
        .addBinding().to(MetricsCollectingEndpointRunnableFactoryDecorator.class);

    manageLifecycle(FastForwardReporter.class);
  }

  @Provides @Singleton
  public SemanticMetricRegistry semanticMetricRegistry() {
    final SemanticMetricRegistry metricRegistry = new SemanticMetricRegistry();
    LOG.info("Creating SemanticMetricRegistry");

    // register JVM metricSets, using an empty MetricId as the FastForwardReporter will prepend
    // the injected MetricId.
    metricRegistry.register(MetricId.EMPTY, new MemoryUsageGaugeSet());
    metricRegistry.register(MetricId.EMPTY, new GarbageCollectorMetricSet());
    metricRegistry.register(MetricId.EMPTY, new ThreadStatesMetricSet());
    metricRegistry.register(MetricId.EMPTY, CpuGaugeSet.create());

    return metricRegistry;
  }

  @Provides @Singleton
  public ApolloMetrics apolloMetrics(SemanticMetricRegistry metricRegistry) {
    return new SemanticApolloMetrics(metricRegistry);
  }

  @Provides @Singleton
  public ApolloServiceMetrics apolloMetrics(
      ApolloMetrics apolloMetrics,
      @Named(Services.INJECT_SERVICE_NAME) String serviceName) {
    return apolloMetrics.createForService(serviceName);
  }

  @Provides @Singleton
  public FastForwardReporter fastForwardReporter(SemanticMetricRegistry metricRegistry,
                                                 MetricId metricId, FfwdConfig ffwdConfig) {
    try {
      final FastForwardReporter.Builder builder = FastForwardReporter.forRegistry(metricRegistry)
          .schedule(TimeUnit.SECONDS, ffwdConfig.getInterval())
          .prefix(metricId);

      ffwdConfig.host().ifPresent(builder::host);
      ffwdConfig.port().ifPresent(builder::port);

      final FastForwardReporter reporter = builder.build();
      reporter.start();
      return reporter;
    } catch (IOException e) {
      throw new IllegalStateException("Failed to start ffwd reporter", e);
    }
  }

  @Override
  public String getId() {
    return "metrics";
  }
}
