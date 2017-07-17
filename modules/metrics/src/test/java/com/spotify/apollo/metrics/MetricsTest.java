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

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.name.Names;

import com.spotify.apollo.core.Services;
import com.spotify.metrics.core.MetricId;
import com.spotify.metrics.core.SemanticMetricFilter;
import com.spotify.metrics.core.SemanticMetricRegistry;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import org.junit.Test;

import static org.junit.Assert.assertFalse;

public class MetricsTest extends AbstractModule {

  @Override
  protected void configure() {
    bindConstant().annotatedWith(Names.named(Services.INJECT_SERVICE_NAME)).to("test");
    bind(Config.class).toInstance(ConfigFactory.empty());
    bind(MetricId.class).toInstance(MetricId.EMPTY);
  }

  @Test
  public void testJvmMetricSets() throws Exception {
    final Injector injector = Guice.createInjector(this, MetricsModule.create());

    final SemanticMetricRegistry metricRegistry = injector.getInstance(SemanticMetricRegistry.class);

    // a little dicey to have to know what tags the MetricSets created metrics with to assert that
    // the Set itself was registered but there is no other obvious way to test these
    assertFalse("Expected MemoryUsageGaugeSet metrics to be registered with registry",
                metricRegistry.getGauges(filterByTag("what", "jvm-memory-usage")).isEmpty());

    assertFalse("Expected GarbageCollectorMetricSet metrics to be registered with registry",
                metricRegistry.getGauges(filterByTag("what", "jvm-gc-collections")).isEmpty());

    assertFalse("Expected ThreadStatesMetricSet metrics to be registered with registry",
                metricRegistry.getGauges(filterByTag("what", "jvm-thread-state")).isEmpty());

    assertFalse("Expected CpuGaugeSet metrics to be registered with registry",
                metricRegistry.getGauges(filterByTag("what", "process-cpu-load-percentage")).isEmpty());

    assertFalse("Expected FileDescriptorRatioGaugeSet metrics to be registered with registry",
                metricRegistry.getGauges(filterByTag("what", "file-descriptor-ratio")).isEmpty());
  }

  private SemanticMetricFilter filterByTag(final String tagName, final String tagValue){
    return (name, metric) -> name.getTags().containsKey(tagName) &&
                             name.getTags().get(tagName).equals(tagValue);
  }
}
